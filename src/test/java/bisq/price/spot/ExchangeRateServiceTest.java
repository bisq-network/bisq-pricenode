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

import bisq.core.locale.CurrencyUtil;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExchangeRateServiceTest {

    /**
     * Logback version of the Slf4j logger used by {@link ExchangeRateService}. This
     * allows us to test if specific messages were logged.
     * See https://stackoverflow.com/a/52229629
     */
    private static Logger exchangeRateServiceLogger;
    private static final String LIST_APPENDER_NAME = "testListAppender";

    @BeforeAll
    static void setup() {
        // Get the logger object for logs in ExchangeRateService
        exchangeRateServiceLogger = (Logger) LoggerFactory.getLogger(ExchangeRateService.class);
        exchangeRateServiceLogger.info("Setup started");

        // Initiate and append a ListAppender, which allows us to programmatically inspect
        // log messages
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setName(LIST_APPENDER_NAME);
        listAppender.start();
        exchangeRateServiceLogger.info("Setup finished");
        exchangeRateServiceLogger.addAppender(listAppender);
    }

    @Test
    public void getAllMarketPrices_withNoExchangeRates_logs_Exception() {
        int numberOfCurrencyPairsOnExchange = 0;
        ExchangeRateProvider dummyProvider = buildDummyExchangeRateProvider(numberOfCurrencyPairsOnExchange);
        Map<String, Object> retrievedData = new ExchangeRateService(
                new StandardEnvironment(), Collections.singletonList(dummyProvider), Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataSingleProvider(retrievedData, dummyProvider, numberOfCurrencyPairsOnExchange);

        // No exchange rates provided by this exchange, two things should happen
        // A) the timestamp should be set to 0
        // B) an error should be logged

        // A) Check that timestamp was set to 0
        // This allows Bisq clients to eventually disregard data from this provider
        assertEquals(0L, retrievedData.get(dummyProvider.getPrefix() + "Ts"));

        // B) Check that an error is logged
        // Log msg has the format: java.lang.IllegalStateException: No exchange rate data
        // found for ExchangeName-JzfP1
        List<ILoggingEvent> logsList = ((ListAppender) exchangeRateServiceLogger.getAppender(LIST_APPENDER_NAME)).list;
        logsList.stream().reduce((first, second) -> second).ifPresentOrElse(loggingEvent -> {
            assertEquals(Level.ERROR, loggingEvent.getLevel());
            assertTrue(loggingEvent.getMessage().endsWith("No exchange rate data found for " + dummyProvider.getName()));
        }, () -> fail("Expected exception log message not found"));
    }

    @Test
    public void getAllMarketPrices_withSingleExchangeRate() {
        int numberOfCurrencyPairsOnExchange = 1;
        ExchangeRateProvider dummyProvider = buildDummyExchangeRateProvider(numberOfCurrencyPairsOnExchange);
        Map<String, Object> retrievedData = new ExchangeRateService(
                new StandardEnvironment(), Collections.singletonList(dummyProvider), Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataSingleProvider(retrievedData, dummyProvider, numberOfCurrencyPairsOnExchange);

        // One rate was provided by this provider, so the timestamp should not be 0
        assertNotEquals(0L, retrievedData.get(dummyProvider.getPrefix() + "Ts"));
    }

    @Test
    public void getAllMarketPrices_withMultipleProviders_differentCurrencyCodes() {
        int numberOfCurrencyPairsOnExchange = 1;
        List<ExchangeRateProvider> providers = asList(
            buildDummyExchangeRateProvider(numberOfCurrencyPairsOnExchange),
            buildDummyExchangeRateProvider(numberOfCurrencyPairsOnExchange));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();

        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationNoOutliers(validateAndGetRetrievedRates(retrievedData), providers);

        // One rate was provided by each provider in this service, so the timestamp
        // (for both providers) should not be 0
        assertNotEquals(0L, retrievedData.get(providers.get(0).getPrefix() + "Ts"));
        assertNotEquals(0L, retrievedData.get(providers.get(1).getPrefix() + "Ts"));
    }

    /**
     * Test scenario
     */
    @Test
    public void getAllMarketPrices_oneProvider_considerBlueUpdates() {
        String excludedCcvString = "LBP";
        Set<String> rateCurrencyCodes = Sets.newHashSet("ARS", "USD", "LBP", "EUR");
        String providerExcludedCcvString = "HUOBI:BRL,BINANCE:GBP,BINANCE:SEK";
        Environment mockedEnvironment = mock(Environment.class);
        when(mockedEnvironment.getProperty(eq("bisq.price.cryptocurrency.excluded"), anyString())).thenReturn("");
        when(mockedEnvironment.getProperty(eq("bisq.price.fiatcurrency.excluded"), anyString())).thenReturn(excludedCcvString);
        when(mockedEnvironment.getProperty(eq("bisq.price.fiatcurrency.excludedByProvider"), anyString())).thenReturn(providerExcludedCcvString);
        ExchangeRateProvider dummyProvider = buildDummyExchangeRateProvider(rateCurrencyCodes, mockedEnvironment);
        ExchangeRateService service = new ExchangeRateService(mockedEnvironment, List.of(dummyProvider), List.of());

        Map<String, Object> retrievedData = service.getAllMarketPrices();

        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, List.of(dummyProvider));

    }

    /**
     * Tests the scenario when multiple providers have rates for the same currencies
     */
    @Test
    public void getAllMarketPrices_withMultipleProviders_overlappingCurrencyCodes() {

        // List of currencies for which multiple providers will have exchange rates
        Set<String> rateCurrencyCodes = Sets.newHashSet("CURRENCY-1", "CURRENCY-2", "CURRENCY-3");

        // Create several dummy providers, each providing their own rates for the same set of currencies
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProvider(rateCurrencyCodes, null),
                buildDummyExchangeRateProvider(rateCurrencyCodes, null));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationNoOutliers(validateAndGetRetrievedRates(retrievedData), providers);

        // At least one rate was provided by each provider in this service, so the
        // timestamp (for both providers) should not be 0
        assertNotEquals(0L, retrievedData.get(providers.get(0).getPrefix() + "Ts"));
        assertNotEquals(0L, retrievedData.get(providers.get(1).getPrefix() + "Ts"));
    }

    @Test
    public void testStaleRatesRemoved() {
        String fiatCoin = "BRL";
        // insert a stale rate
        long staleTimestamp = 10000L;
        Long validTimestamp = System.currentTimeMillis();
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRateAndTimestamp("mercadoBitcoin", fiatCoin, 129000.0, staleTimestamp),
                buildDummyExchangeRateProviderWithRateAndTimestamp("coinGecko", fiatCoin, 129000.0, validTimestamp),
                buildDummyExchangeRateProviderWithRateAndTimestamp("binance", fiatCoin, 131000.0, validTimestamp));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        // check that the provider's stale rate was removed resulting in 0 rates and a 0 timestamp
        assertEquals("0", retrievedData.get("mercadoBitcoinTs").toString());
        assertEquals("0", retrievedData.get("mercadoBitcoinCount").toString());
        // check that other providers are unaffected
        assertEquals("1", retrievedData.get("coinGeckoCount").toString());
        assertEquals("1", retrievedData.get("binanceCount").toString());
        assertEquals(validTimestamp.toString(), retrievedData.get("coinGeckoTs").toString());
        assertEquals(validTimestamp.toString(), retrievedData.get("binanceTs").toString());
    }

    @Test
    public void bisqIndexCalculation_oneOutlierPriceWideRange() {
        String fiatCoin = "BRL";
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRate("mercadoBitcoin", fiatCoin, 0.0),  // outlier - low
                buildDummyExchangeRateProviderWithRate("coinGecko", fiatCoin, 129000.0),
                buildDummyExchangeRateProviderWithRate("binance", fiatCoin, 131000.0));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationWithOutlier(validateAndGetRetrievedRates(retrievedData), providers, Collections.singletonList("mercadoBitcoin"));
    }

    @Test
    public void bisqIndexCalculation_oneOutlierPriceCloseRange() {
        String fiatCoin = "BRL";
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRate("mercadoBitcoin", fiatCoin, 124545.5),
                buildDummyExchangeRateProviderWithRate("coinGecko", fiatCoin, 124083.752),
                buildDummyExchangeRateProviderWithRate("binance", fiatCoin, 124726.0));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationWithOutlier(validateAndGetRetrievedRates(retrievedData), providers, Collections.singletonList("coinGecko"));
    }

    @Test
    public void bisqIndexCalculation_twoOutlierPrices() {
        String fiatCoin = "GBP";
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRate("bitstamp", fiatCoin, 20788.0),
                buildDummyExchangeRateProviderWithRate("bitfinex", fiatCoin, 19780.0), // outlier - low
                buildDummyExchangeRateProviderWithRate("kraken", fiatCoin, 20795.0),
                buildDummyExchangeRateProviderWithRate("coinGecko", fiatCoin, 20774.0),
                buildDummyExchangeRateProviderWithRate("binance", fiatCoin, 21361.48), // outlier - high
                buildDummyExchangeRateProviderWithRate("coinbasePro", fiatCoin, 20798.25));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationWithOutlier(validateAndGetRetrievedRates(retrievedData), providers, List.of("binance", "bitfinex"));
    }

    @Test
    public void bisqIndexCalculation_altcoinOutlierPrices() {
        String altcoin = "ETH";
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRate("bitflyer", altcoin, 0.06262),
                buildDummyExchangeRateProviderWithRate("bitstamp", altcoin, 0.0625939),
                buildDummyExchangeRateProviderWithRate("kraken", altcoin, 0.06259),
                buildDummyExchangeRateProviderWithRate("coinGecko", altcoin, 0.06264487),
                buildDummyExchangeRateProviderWithRate("binance", altcoin, 0.05),   // outlier - low
                buildDummyExchangeRateProviderWithRate("poloniex", altcoin, 0.07),  // outlier - high
                buildDummyExchangeRateProviderWithRate("coinbasePro", altcoin, 0.06259));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationWithOutlier(validateAndGetRetrievedRates(retrievedData), providers, List.of("binance", "poloniex"));
    }

    @Test
    public void bisqIndexCalculation_allSamePricesAltcoin() {
        String altcoin = "DOGE";
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRate("bitfinex", altcoin, 2.37E-6),
                buildDummyExchangeRateProviderWithRate("kraken", altcoin, 2.37E-6),
                buildDummyExchangeRateProviderWithRate("coinbasePro", altcoin, 2.37E-6));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationNoOutliers(validateAndGetRetrievedRates(retrievedData), providers);
    }

    @Test
    public void bisqIndexCalculation_allSamePricesFiat() {
        String fiat = "USD";
        List<ExchangeRateProvider> providers = asList(
                buildDummyExchangeRateProviderWithRate("bitfinex", fiat, 10000.0),
                buildDummyExchangeRateProviderWithRate("kraken", fiat, 10000.0),
                buildDummyExchangeRateProviderWithRate("coinbasePro", fiat, 10000.0));
        Map<String, Object> retrievedData = new ExchangeRateService(new StandardEnvironment(), providers, Collections.emptyList()).getAllMarketPrices();
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, providers);
        checkBisqIndexCalculationNoOutliers(validateAndGetRetrievedRates(retrievedData), providers);
    }

    /**
     * Tests the scenario when currencies are excluded from the PriceNode feed via configuration settings
     */
    @Test
    public void getAllMarketPrices_withMultipleProviders_excludedCurrencyCodes() {
        String excludedCcyString = "LBP,USD,EUR";
        String providerExcludedCcyString = "HUOBI:BRL,BINANCE:GBP,BINANCE:SEK";
        Environment mockedEnvironment = mock(Environment.class);
        when(mockedEnvironment.getProperty(eq("bisq.price.fiatcurrency.excluded"), anyString())).thenReturn(excludedCcyString);
        when(mockedEnvironment.getProperty(eq("bisq.price.fiatcurrency.excludedByProvider"), anyString())).thenReturn(providerExcludedCcyString);

        class MockedExchangeRateProvider extends ExchangeRateProvider {
            MockedExchangeRateProvider() {
                super(mockedEnvironment, "ExchangeName", "EXCH", Duration.ofDays(1));
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public Set<ExchangeRate> doGet() {
                HashSet<ExchangeRate> exchangeRates = new HashSet<>();
                // Simulate rates for all the supported ccys
                for (String rateCurrencyCode : getSupportedFiatCurrencies()) {
                    exchangeRates.add(new ExchangeRate(
                            rateCurrencyCode,
                            RandomUtils.nextDouble(1, 1000), // random price
                            System.currentTimeMillis(),
                            getName())); // ExchangeRateProvider name
                }
                return exchangeRates;
            }
        }

        Logger exchangeRateProviderLogger;
        String LIST_APPENDER_NAME2 = "testListAppender2";
        exchangeRateProviderLogger = (Logger) LoggerFactory.getLogger(MockedExchangeRateProvider.class);
        ListAppender<ILoggingEvent> listAppender2 = new ListAppender<>();
        listAppender2.setName(LIST_APPENDER_NAME2);
        listAppender2.start();
        exchangeRateProviderLogger.addAppender(listAppender2);

        // we request rates for all currencies, and check that:
        //   (a) the provider supplies more currency rates than the number of currencies we are trying to exclude (sanity test),
        //   (b) the number of missing currency rates equals the number of currencies we told PriceNode to exclude,
        //   (c) none of the rates supplied are for an excluded currency.

        Set<String> excludedFiatCurrencies = new HashSet<>(asList(excludedCcyString.split(",")));
        MockedExchangeRateProvider mockedExchangeRateProvider = new MockedExchangeRateProvider();
        Set<ExchangeRate> exchangeRates = mockedExchangeRateProvider.doGet();
        assertTrue(exchangeRates.size() > excludedFiatCurrencies.size());
        int numSortedFiatCurrencies = CurrencyUtil.getAllSortedFiatCurrencies().size();
        int numCurrenciesFromProvider = mockedExchangeRateProvider.getSupportedFiatCurrencies().size();
        int missingCurrencyCount = numSortedFiatCurrencies - numCurrenciesFromProvider;
        assertEquals(missingCurrencyCount, excludedFiatCurrencies.size());
        for (ExchangeRate exchangeRate : exchangeRates) {
            assertFalse(excludedCcyString.contains(exchangeRate.getCurrency()));
        }
        List<ILoggingEvent> logsList = ((ListAppender) exchangeRateProviderLogger.getAppender(LIST_APPENDER_NAME2)).list;
        assertEquals(3, logsList.size());
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertTrue(logsList.get(0).getFormattedMessage().endsWith("will refresh every PT24H"));
        assertTrue(logsList.get(1).getFormattedMessage().endsWith("fiat currencies excluded: [LBP, USD, EUR]"));
        assertTrue(logsList.get(2).getFormattedMessage().endsWith("fiat currencies supported: " + numCurrenciesFromProvider));
    }

    /**
     * Performs generic sanity checks on the response format and contents.
     *
     * @param retrievedData                   Response data retrieved from the {@link ExchangeRateService}
     * @param provider                        {@link ExchangeRateProvider} available to the
     *                                        {@link ExchangeRateService}
     * @param numberOfCurrencyPairsOnExchange Number of currency pairs this exchange was
     *                                        initiated with
     */
    private void doSanityChecksForRetrievedDataSingleProvider(Map<String, Object> retrievedData,
                                                              ExchangeRateProvider provider,
                                                              int numberOfCurrencyPairsOnExchange) {
        // Check response structure
        doSanityChecksForRetrievedDataMultipleProviders(retrievedData, asList(provider));

        // Check that the amount of provided exchange rates matches expected value
        // For one provider, the amount of rates of that provider should be the total
        // amount of rates in the response
        List<ExchangeRate> retrievedRates = (List<ExchangeRate>) retrievedData.get("data");
        assertEquals(numberOfCurrencyPairsOnExchange, retrievedRates.size());
    }


    private List<ExchangeRate> validateAndGetRetrievedRates(Map<String, Object> retrievedData) {
        // Check validity of the data field
        List<ExchangeRate> retrievedRates = (List<ExchangeRate>) retrievedData.get("data");
        assertNotNull(retrievedRates);

        // It should contain no duplicate ExchangeRate objects
        int uniqueRates = Sets.newHashSet(retrievedRates).size();
        int totalRates = retrievedRates.size();
        assertEquals(uniqueRates, totalRates, "Found duplicate rates in data field");

        // There should be only one ExchangeRate per currency
        // In other words, even if multiple providers return rates for the same currency,
        // the ExchangeRateService should expose only one (aggregate) ExchangeRate for
        // that currency
        Map<String, ExchangeRate> currencyCodeToExchangeRateFromService = retrievedRates.stream()
                .collect(Collectors.toMap(
                        ExchangeRate::getCurrency, exchangeRate -> exchangeRate
                ));
        int uniqueCurrencyCodes = currencyCodeToExchangeRateFromService.keySet().size();
        assertEquals(uniqueCurrencyCodes, uniqueRates, "Found currency code with multiple exchange rates");

        return retrievedRates;
    }

    /**
     * Performs generic sanity checks on the response format and contents.
     *
     * @param retrievedData Response data retrieved from the {@link ExchangeRateService}
     * @param providers     List of all {@link ExchangeRateProvider#getPrefix()} the
     *                      {@link ExchangeRateService} uses
     */
    private void doSanityChecksForRetrievedDataMultipleProviders(Map<String, Object> retrievedData,
                                                                 List<ExchangeRateProvider> providers) {
        // Check the correct amount of entries were present in the service response:
        // The timestamp and the count fields are per provider, so N providers means N
        // times those fields timestamp (x N) + count (x N) + price data (stored as a list
        // under the key "data"). So expected size is Nx2 + 1.
        int n = providers.size();
        assertEquals(n * 2 + 1, retrievedData.size());
        for (ExchangeRateProvider provider : providers) {
            String providerPrefix = provider.getPrefix();
            assertNotNull(retrievedData.get(providerPrefix + "Ts"));
            assertNotNull(retrievedData.get(providerPrefix + "Count"));
        }
    }

    private void checkBisqIndexCalculationNoOutliers(List<ExchangeRate> retrievedData,
                                                      List<ExchangeRateProvider> providers) {
        checkBisqIndexCalculation(retrievedData, providers, Collections.emptyList());
    }

    private void checkBisqIndexCalculationWithOutlier(List<ExchangeRate> retrievedData,
                                           List<ExchangeRateProvider> providers,
                                           List<String> outlierProviderNames) {
        checkBisqIndexCalculation(retrievedData, providers, outlierProviderNames);
    }

    // checks that the bisq index equals the average of all retrieved quotes
    private void checkBisqIndexCalculation(List<ExchangeRate> retrievedData,
                                           List<ExchangeRateProvider> providers,
                                           List<String> optionalOutlierProviderNames) {
        Map<String, ExchangeRate> currencyCodeToExchangeRateFromService = retrievedData.stream()
                .collect(Collectors.toMap(
                        ExchangeRate::getCurrency, exchangeRate -> exchangeRate
                ));
        Map<String, List<ExchangeRate>> currencyCodeToExchangeRatesFromProviders = new HashMap<>();
        for (ExchangeRateProvider p : providers) {
            for (ExchangeRate exchangeRate : p.get()) {
                String currencyCode = exchangeRate.getCurrency();
                if (currencyCodeToExchangeRatesFromProviders.containsKey(currencyCode)) {
                    List<ExchangeRate> l = new ArrayList<>(currencyCodeToExchangeRatesFromProviders.get(currencyCode));
                    l.add(exchangeRate);
                    currencyCodeToExchangeRatesFromProviders.put(currencyCode, l);
                } else {
                    currencyCodeToExchangeRatesFromProviders.put(currencyCode, Collections.singletonList(exchangeRate));
                }
            }
        }
        // For each ExchangeRate which is covered by multiple providers, ensure the rate
        // value is an average
        currencyCodeToExchangeRatesFromProviders.forEach((currencyCode, exchangeRateList) -> {
            ExchangeRate actualBisqIndex = currencyCodeToExchangeRateFromService.get(currencyCode);
            OptionalDouble testBisqIndex = exchangeRateList.stream()
                    .filter(e -> !optionalOutlierProviderNames.contains(e.getProvider()))
                    .mapToDouble(ExchangeRate::getPrice)
                    .average();
            // Ensure that the ExchangeRateService correctly aggregates exchange rates
            // from multiple providers. If multiple providers contain rates for a
            // currency, the service should return a single aggregate rate
            // Expected value for aggregate rate = avg(provider rates)
            // This formula works for any number of providers for a specific currency
            assertEquals(testBisqIndex.getAsDouble(), actualBisqIndex.getPrice(), "Service returned incorrect aggregate rate");
        });
    }


    /**
     * @param numberOfRatesAvailable Number of exchange rates this provider returns
     * @return Dummy {@link ExchangeRateProvider} providing rates for
     * "numberOfRatesAvailable" random currency codes
     */
    private ExchangeRateProvider buildDummyExchangeRateProvider(int numberOfRatesAvailable) {
        ExchangeRateProvider dummyProvider = new ExchangeRateProvider(
                new StandardEnvironment(),
                "ExchangeName-" + getRandomAlphaNumericString(5),
                "EXCH-" + getRandomAlphaNumericString(3),
                Duration.ofDays(1)) {

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            protected Set<ExchangeRate> doGet() {
                HashSet<ExchangeRate> exchangeRates = new HashSet<>();

                // Simulate the required amount of rates
                for (int i = 0; i < numberOfRatesAvailable; i++) {
                    exchangeRates.add(new ExchangeRate(
                            // random symbol, avoid duplicates
                            "DUM-" + getRandomAlphaNumericString(3),
                            RandomUtils.nextDouble(1, 1000), // random price
                            System.currentTimeMillis(),
                            getName())); // ExchangeRateProvider name
                }

                return exchangeRates;
            }
        };

        // Initialize provider
        dummyProvider.start();
        try {
            sleep(1000);
        } catch (InterruptedException e) {
        }
        dummyProvider.stop();

        return dummyProvider;
    }

    private ExchangeRateProvider buildDummyExchangeRateProvider(Set<String> rateCurrencyCodes, @Nullable Environment env) {
        ExchangeRateProvider dummyProvider = new ExchangeRateProvider(
                env == null ? new StandardEnvironment() : env,
                "ExchangeName-" + getRandomAlphaNumericString(5),
                "EXCH-" + getRandomAlphaNumericString(3),
                Duration.ofDays(1)) {

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            protected Set<ExchangeRate> doGet() {
                HashSet<ExchangeRate> exchangeRates = new HashSet<>();

                // Simulate the required amount of rates
                for (String rateCurrencyCode : rateCurrencyCodes) {
                    exchangeRates.add(new ExchangeRate(
                            rateCurrencyCode,
                            RandomUtils.nextDouble(1, 1000), // random price
                            System.currentTimeMillis(),
                            getName())); // ExchangeRateProvider name
                }

                return exchangeRates;
            }
        };

        // Initialize provider
        dummyProvider.start();
        try {
            sleep(1000);
        } catch (InterruptedException e) {
        }
        dummyProvider.stop();

        return dummyProvider;
    }

    private ExchangeRateProvider buildDummyExchangeRateProviderWithRate(
            String providerName, String currencyCode, Double rate) {
        return buildDummyExchangeRateProviderWithRateAndTimestamp(
                providerName, currencyCode, rate, System.currentTimeMillis());
    }
    private ExchangeRateProvider buildDummyExchangeRateProviderWithRateAndTimestamp(
            String providerName, String currencyCode, Double rate, long timestamp) {
        ExchangeRateProvider dummyProvider = new ExchangeRateProvider(
                new StandardEnvironment(),
                providerName,
                providerName,
                Duration.ofDays(1)) {

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            protected Set<ExchangeRate> doGet() {
                HashSet<ExchangeRate> exchangeRates = new HashSet<>();
                exchangeRates.add(new ExchangeRate(
                        currencyCode,
                        rate,
                        timestamp,
                        getName())); // ExchangeRateProvider name
                return exchangeRates;
            }
        };

        // Initialize provider
        dummyProvider.start();
        try {
            sleep(1000);
        } catch (InterruptedException e) {
        }
        dummyProvider.stop();
        return dummyProvider;
    }

    private static String getRandomAlphaNumericString(int length) {
        return RandomStringUtils.random(length, true, true);
    }
}
