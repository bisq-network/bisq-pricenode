/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.price.spot;

import bisq.price.common.util.InlierUtil;
import bisq.price.common.util.Tuple2;
import bisq.price.util.GatedLogging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-level {@link ExchangeRate} data operations.
 */
@Service
@Slf4j
class ExchangeRateService {
    private final Environment env;
    private final List<ExchangeRateProvider> providers;
    private final List<ExchangeRateTransformer> transformers;
    private final GatedLogging gatedLogging = new GatedLogging();

    /**
     * Construct an {@link ExchangeRateService} with a list of all
     * {@link ExchangeRateProvider} implementations discovered via classpath scanning.
     *
     * @param providers    all {@link ExchangeRateProvider} implementations in ascending
     *                     order of precedence
     * @param transformers all {@link ExchangeRateTransformer} implementations
     */
    public ExchangeRateService(Environment env,
                               List<ExchangeRateProvider> providers,
                               List<ExchangeRateTransformer> transformers) {
        this.env = env;
        this.providers = providers;
        this.transformers = transformers;
    }

    public Map<String, Object> getAllMarketPrices() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, ExchangeRate> aggregateExchangeRates = getAggregateExchangeRates();

        providers.forEach(p -> {
            p.maybeClearStaleRates();
            // Specific metadata fields for specific providers are expected by the client,
            // mostly for historical reasons
            // Therefore, add metadata fields for all known providers
            // Rates are encapsulated in the "data" map below
            metadata.putAll(getMetadata(p));
        });


        LinkedHashMap<String, Object> result = new LinkedHashMap<>(metadata);
        // Use a sorted list by currency code to make comparison of json data between
        // different price nodes easier
        List<ExchangeRate> values = new ArrayList<>(aggregateExchangeRates.values());
        values.sort(Comparator.comparing(ExchangeRate::getCurrency));
        result.put("data", values);

        return result;
    }

    /**
     * For each currency, create an aggregate {@link ExchangeRate} based on the currency's
     * rates from all providers. If multiple providers have rates for the currency, then
     * aggregate price = average of retrieved prices. If a single provider has rates for
     * the currency, then aggregate price = the rate from that provider.
     *
     * @return Aggregate {@link ExchangeRate}s based on info from all providers, indexed
     * by currency code
     */
    private Map<String, ExchangeRate> getAggregateExchangeRates() {
        boolean maybeLogDetails = gatedLogging.gatingOperation();
        Map<String, ExchangeRate> aggregateExchangeRates = new HashMap<>();

        // Query all providers and collect all exchange rates, grouped by currency code
        // key = currency code
        // value = list of exchange rates
        Map<String, List<ExchangeRate>> currencyCodeToExchangeRates = getCurrencyCodeToExchangeRates();

        // For each currency code, calculate aggregate rate
        currencyCodeToExchangeRates.forEach((currencyCode, exchangeRateList) -> {
            if (exchangeRateList.isEmpty()) {
                // If the map was built incorrectly and this currency points to an empty
                // list of rates, skip it
                return;
            }

            ExchangeRate aggregateExchangeRate;
            if (exchangeRateList.size() == 1) {
                // If a single provider has rates for this currency, then aggregate = rate
                // from that provider
                aggregateExchangeRate = exchangeRateList.getFirst();
            } else {
                // If multiple providers have rates for this currency, then
                // aggregate = average of the rates
                double priceAvg = priceAverageWithOutliersRemoved(exchangeRateList, currencyCode, maybeLogDetails);
                aggregateExchangeRate = new ExchangeRate(
                        currencyCode,
                        BigDecimal.valueOf(priceAvg),
                        new Date(), // timestamp = time when avg is calculated
                        "Bisq-Aggregate");
            }
            aggregateExchangeRates.put(aggregateExchangeRate.getCurrency(), aggregateExchangeRate);
        });

        return aggregateExchangeRates;
    }

    private double priceAverageWithOutliersRemoved(
            List<ExchangeRate> exchangeRateList, String contextInfo, boolean logOutliers) {
        final List<Double> yValues = exchangeRateList.stream().
                mapToDouble(ExchangeRate::getPrice).boxed().collect(Collectors.toList());
        Tuple2<Double, Double> tuple = InlierUtil.findInlierRange(yValues, 0, getOutlierStdDeviation());
        double lowerBound = tuple.first;
        double upperBound = tuple.second;
        final List<ExchangeRate> filteredPrices = exchangeRateList.stream()
                .filter(e -> e.getPrice() >= lowerBound)
                .filter(e -> e.getPrice() <= upperBound)
                .toList();

        if (filteredPrices.isEmpty()) {
            log.error("{}: could not filter, revert to plain average. lowerBound={}, upperBound={}, stdDev={}, yValues={}",
                    contextInfo, lowerBound, upperBound, getOutlierStdDeviation(), yValues);
            return exchangeRateList.stream().mapToDouble(ExchangeRate::getPrice).average().getAsDouble();
        }

        OptionalDouble opt = filteredPrices.stream().mapToDouble(ExchangeRate::getPrice).average();
        // List size > 1, so opt is always set
        double priceAvg = opt.orElseThrow(IllegalStateException::new);

        // log the outlier prices which were removed from the average, if any.
        if (logOutliers) {
            for (ExchangeRate badRate : exchangeRateList.stream()
                    .filter(e -> !filteredPrices.contains(e))
                    .toList()) {
                log.info("{} {} outlier price removed:{}, lower/upper bounds:{}/{}, consensus price:{}",
                        badRate.getProvider(),
                        badRate.getCurrency(),
                        badRate.getPrice(),
                        lowerBound,
                        upperBound,
                        priceAvg);
            }
        }
        return priceAvg;
    }

    private double getOutlierStdDeviation() {
        return Double.parseDouble(env.getProperty("bisq.price.outlierStdDeviation", "1.1"));
    }

    /**
     * @return All {@link ExchangeRate}s from all providers, grouped by currency code
     */
    private Map<String, List<ExchangeRate>> getCurrencyCodeToExchangeRates() {
        Map<String, List<ExchangeRate>> currencyCodeToExchangeRates = new HashMap<>();
        for (ExchangeRateProvider p : providers) {
            Set<ExchangeRate> exchangeRates = p.get();
            if (exchangeRates == null)
                continue;
            for (ExchangeRate exchangeRate : exchangeRates) {
                String currencyCode = exchangeRate.getCurrency();

                List<ExchangeRate> finalExchangeRates = transformers.stream()
                        .filter(transformer -> transformer.supportedCurrency()
                                .equalsIgnoreCase(currencyCode)
                        )
                        .map(t -> t.apply(p, exchangeRate))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                if (currencyCodeToExchangeRates.containsKey(currencyCode)) {
                    List<ExchangeRate> l = new ArrayList<>(currencyCodeToExchangeRates.get(currencyCode));
                    if (finalExchangeRates.isEmpty()) {
                        l.add(exchangeRate);
                    } else {
                        l.addAll(finalExchangeRates);
                    }
                    currencyCodeToExchangeRates.put(currencyCode, l);
                } else {
                    if (finalExchangeRates.isEmpty()) {
                        currencyCodeToExchangeRates.put(currencyCode, List.of(exchangeRate));
                    } else {
                        currencyCodeToExchangeRates.put(currencyCode, finalExchangeRates);
                    }
                }
            }
        }

        return currencyCodeToExchangeRates;
    }

    private Map<String, Object> getMetadata(ExchangeRateProvider provider) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        // In case a provider is not available we still want to deliver the data of the
        // other providers, so we catch a possible exception and leave timestamp at 0. The
        // Bisq app will check if the timestamp is in a tolerance window and if it is too
        // old it will show that the price is not available.
        long timestamp = 0;
        Set<ExchangeRate> exchangeRates = provider.get();
        try {
            if (exchangeRates != null) {
                timestamp = getTimestamp(provider, exchangeRates);
            }
        } catch (Throwable t) {
            log.error(t.toString());
            if (log.isDebugEnabled())
                t.printStackTrace();
        }

        String prefix = provider.getPrefix();
        metadata.put(prefix + "Ts", timestamp);
        metadata.put(prefix + "Count", exchangeRates == null ? 0 : exchangeRates.size());

        return metadata;
    }

    private long getTimestamp(ExchangeRateProvider provider, Set<ExchangeRate> exchangeRates) {
        return exchangeRates.stream()
                .filter(e -> e.getProvider().startsWith(provider.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No exchange rate data found for " + provider.getName()))
                .getTimestamp();
    }
}
