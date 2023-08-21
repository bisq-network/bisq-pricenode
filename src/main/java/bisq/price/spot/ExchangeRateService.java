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

import bisq.common.util.Tuple2;

import bisq.core.util.InlierUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-level {@link ExchangeRate} data operations.
 */
@Service
class ExchangeRateService {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Environment env;
    private final List<ExchangeRateProvider> providers;

    /**
     * Construct an {@link ExchangeRateService} with a list of all
     * {@link ExchangeRateProvider} implementations discovered via classpath scanning.
     *
     * @param providers all {@link ExchangeRateProvider} implementations in ascending
     *                  order of precedence
     */
    public ExchangeRateService(Environment env, List<ExchangeRateProvider> providers) {
        this.env = env;
        this.providers = providers;
    }

    public Map<String, Object> getAllMarketPrices() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, ExchangeRate> aggregateExchangeRates = getAggregateExchangeRates();

        providers.forEach(p -> {
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
                aggregateExchangeRate = exchangeRateList.get(0);
            } else {
                // If multiple providers have rates for this currency, then
                // aggregate = average of the rates
                List<Double> goodPriceList = removeOutliers(exchangeRateList.stream().
                        mapToDouble(ExchangeRate::getPrice).boxed().collect(Collectors.toList()), currencyCode);
                OptionalDouble opt = goodPriceList.stream().mapToDouble(Double::doubleValue).average();
                // List size > 1, so opt is always set
                double priceAvg = opt.orElseThrow(IllegalStateException::new);
                aggregateExchangeRate = new ExchangeRate(
                        currencyCode,
                        BigDecimal.valueOf(priceAvg),
                        new Date(), // timestamp = time when avg is calculated
                        "Bisq-Aggregate");
                // log the outlier prices which were removed from the average, if any.
                for (ExchangeRate badRate : exchangeRateList.stream()
                        .filter(e -> !goodPriceList.contains(e.getPrice()))
                        .collect(Collectors.toList())) {
                    log.warn("outlier price removed={}, source={}, ccy={}, consensus price={}",
                            badRate.getPrice(),
                            badRate.getProvider(),
                            currencyCode,
                            aggregateExchangeRate.getPrice());
                }
            }
            aggregateExchangeRates.put(aggregateExchangeRate.getCurrency(), aggregateExchangeRate);
        });

        return aggregateExchangeRates;
    }

    private List<Double> removeOutliers(List<Double> yValues, String contextInfo) {
        Tuple2<Double, Double> tuple = InlierUtil.findInlierRange(yValues, 0, getOutlierStdDeviation());
        double lowerBound = tuple.first;
        double upperBound = tuple.second;
        List<Double> filteredPrices = yValues.stream()
                .filter(e -> e >= lowerBound)
                .filter(e -> e <= upperBound)
                .collect(Collectors.toList());
        if (filteredPrices.size() < 1) {
            log.error("{}: no results after outliers removed. lowerBound={}, upperBound={}, stdDev={}, yValues={}",
                    contextInfo, lowerBound, upperBound, getOutlierStdDeviation(), yValues.toString());
            return yValues;   // all prices cannot be removed, so revert to keep service running
        }
        return filteredPrices;
    }

    private double getOutlierStdDeviation() {
        return Double.parseDouble(env.getProperty("bisq.price.outlierStdDeviation", "2.2"));
    }

    /**
     * @return All {@link ExchangeRate}s from all providers, grouped by currency code
     */
    private Map<String, List<ExchangeRate>> getCurrencyCodeToExchangeRates() {
        Map<String, List<ExchangeRate>> currencyCodeToExchangeRates = new HashMap<>();
        for (ExchangeRateProvider p : providers) {
            if (p.get() == null)
                continue;
            for (ExchangeRate exchangeRate : p.get()) {
                String currencyCode = exchangeRate.getCurrency();
                if (currencyCodeToExchangeRates.containsKey(currencyCode)) {
                    List<ExchangeRate> l = new ArrayList<>(currencyCodeToExchangeRates.get(currencyCode));
                    l.add(exchangeRate);
                    currencyCodeToExchangeRates.put(currencyCode, l);
                } else {
                    currencyCodeToExchangeRates.put(currencyCode, List.of(exchangeRate));
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
                .filter(e -> provider.getName().equals(e.getProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No exchange rate data found for " + provider.getName()))
                .getTimestamp();
    }
}
