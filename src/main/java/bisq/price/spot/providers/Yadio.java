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

package bisq.price.spot.providers;

import bisq.price.spot.ExchangeRate;
import bisq.price.spot.ExchangeRateProvider;
import bisq.price.util.yadio.YadioTicker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.boot.context.properties.bind.Bindable.setOf;

/**
 * Yadio is used for "marginal market" currencies. Originally thought for:
 * <ul>
 *     <li>PYG (Paraguayan Guarani</li>
 *     <li>DOP (Dominican Peso)</li>
 *     <li>BOB (Bolivian Bolivariano</li>
 *     <li>EGP (Egyptian Pound</li>
 * </ul>
 * The API uses real market data ignoring official goverment official currency rates, therefore this class also implements BlueRateProvider
 * and its used to provide price points for:
 *
 *  <ul>
 *      <li>ARS (Argentine Peso)</li>
 *      <li>LBP (Lebanese Pound)</li>
 *  </ul>
 *
 * Further analysis could be made to incorporate more currencies like VES (Venezuela).
 */
@Component
public class Yadio extends ExchangeRateProvider implements BlueRateProvider {

    public static final String PROVIDER_NAME = "YADIO";

    private static final String YADIO_EXCHANGES_API_ENDPOINT = "https://api.yadio.io/exrates";

    private static final Set<String> YADIO_CURRENCIES_WHITELIST = Set.of("ARS", "BOB", "DOP", "EGP", "LBP", "PYG");

    private final WebClient webClient = WebClient.create();

    public Yadio(Environment env) {
        super(env, PROVIDER_NAME, "yadio", Duration.ofMinutes(1));
    }

    /**
     * @return average price buy/sell price averaging different providers suported by yadio api
     */
    @Override
    public Set<ExchangeRate> doGet() {
        YadioTicker yadioTicker = getYadioTicker();

        Instant yesterdayInstant = Instant.now()
                .minus(1, ChronoUnit.DAYS);

        if (!yadioTicker.isValid(yesterdayInstant)) {
            return Collections.emptySet();
        }

        return yadioTicker.getUSD().entrySet()
                .stream()
                .filter(entry -> YADIO_CURRENCIES_WHITELIST.contains(entry.getKey()))
                .map(yadioEntryToExchangeRate(yadioTicker))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    private YadioTicker getYadioTicker() {
        Map<String, Object> yadioMarketData = fetchBaseMarketData();
        // Had to manually parse the json to avoid a double request,
        // won't pick the map structure properly of the USD field
        YadioTicker yadioTicker = new YadioTicker();
        yadioTicker.setBTC((Double) yadioMarketData.get("BTC"));
        yadioTicker.setBase((String) yadioMarketData.get("base"));
        yadioTicker.setTimestamp((Long) yadioMarketData.get("timestamp"));
        @SuppressWarnings("unchecked")
        Map<String, Double> usdMap = (Map<String, Double>) yadioMarketData.getOrDefault("USD", Collections.emptyMap());
        yadioTicker.setUSD(usdMap);
        return yadioTicker;
    }

    private Map<String, Object> fetchBaseMarketData() {
        return webClient.get()
                .uri(YADIO_EXCHANGES_API_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.of(30, ChronoUnit.SECONDS));
    }

    private Function<Map.Entry<String, Double>, Optional<ExchangeRate>> yadioEntryToExchangeRate(YadioTicker ticker) {
        return entry -> {
            try {
                String currencySymbol = entry.getKey();
                double priceInUSD = ((Number) entry.getValue()).doubleValue();
                double priceInBTC = priceInUSD * ticker.getBTC();
                return toExchangeRate(currencySymbol, priceInBTC);
            } catch (Exception e) {
                log.error("Failed to parse price rate for currency {}", entry.getKey(), e);
                return Optional.empty();
            }
        };
    }

    private Optional<ExchangeRate> toExchangeRate(String currencySymbol, double price) {
        return Optional.of(
                new ExchangeRate(
                        currencySymbol,
                        price,
                        System.currentTimeMillis(),
                        Yadio.PROVIDER_NAME
                )
        );
    }
}
