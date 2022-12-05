package haveno.price;

import haveno.price.spot.ExchangeRate;
import haveno.price.spot.ExchangeRateProvider;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public abstract class AbstractExchangeRateProviderTest {

    protected void doGet_successfulCall(ExchangeRateProvider exchangeProvider) {

        // Use the XChange library to call the provider API, in order to retrieve the
        // exchange rates. If the API call fails, or the response body cannot be parsed,
        // the test will fail with an exception
        Set<ExchangeRate> retrievedExchangeRates = exchangeProvider.doGet();

        // Log the valid exchange rates which were retrieved
        // Useful when running the tests, to easily identify which exchanges provide
        // useful pairs
        retrievedExchangeRates.forEach(e -> log.info("Found exchange rate " + e.toString()));

        // Sanity checks
        assertTrue(retrievedExchangeRates.size() > 0);
        checkProviderCurrencyPairs(exchangeProvider, retrievedExchangeRates);
    }

    /**
     * Check that every retrieved currency pair is between BTC and either
     * A) a fiat currency on the list of Haveno-supported fiat currencies, or
     * B) an altcoin on the list of Haveno-supported altcoins
     *
     * @param retrievedExchangeRates Exchange rates retrieved from the provider
     */
    private void checkProviderCurrencyPairs(ExchangeRateProvider exchangeProvider, Set<ExchangeRate> retrievedExchangeRates) {
        Set<String> retrievedRatesCurrencies = retrievedExchangeRates.stream()
                .map(ExchangeRate::getCurrency)
                .collect(Collectors.toSet());

        Set<String> supportedFiatCurrenciesRetrieved = exchangeProvider.getSupportedFiatCurrencies().stream()
                .filter(f -> retrievedRatesCurrencies.contains(f))
                .collect(Collectors.toCollection(TreeSet::new));
        log.info("Retrieved rates for supported fiat currencies: " + supportedFiatCurrenciesRetrieved);

        Set<String> supportedCryptoCurrenciesRetrieved = exchangeProvider.getSupportedCryptoCurrencies().stream()
                .filter(c -> retrievedRatesCurrencies.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        log.info("Retrieved rates for supported altcoins: " + supportedCryptoCurrenciesRetrieved);

        Set<String> supportedCurrencies = Sets.union(
                exchangeProvider.getSupportedCryptoCurrencies(),
                exchangeProvider.getSupportedFiatCurrencies());

        Set unsupportedCurrencies = Sets.difference(retrievedRatesCurrencies, supportedCurrencies);
        assertTrue(unsupportedCurrencies.isEmpty(),
                "Retrieved exchange rates contain unsupported currencies: " + unsupportedCurrencies);
    }
}
