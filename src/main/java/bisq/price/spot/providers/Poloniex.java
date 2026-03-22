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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class Poloniex extends ExchangeRateProvider {
    private static final List<String> SUPPORTED_CURRENCIES =
            List.of("DASH", "DCR", "DOGE", "ETC", "ETH", "LTC", "XMR", "ZEC");
    private static final String POLONIEX_URL = "https://api.poloniex.com/markets/price";
    private static final String PROVIDER_NAME = "POLO";
    public Poloniex(Environment env) {
        super(env, PROVIDER_NAME, "poloniex", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {
        Flux<PoloniexTicker> poloniexTickerFlux = WebClient.create()
                .get()
                .uri(POLONIEX_URL)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(PoloniexTicker.class);

        return poloniexTickerFlux.filter(PoloniexTicker::isSupportedCurrency)
                .map(PoloniexTicker::toExchangeRate)
                .collect(Collectors.toSet())
                .block();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PoloniexTicker {
        public String symbol;
        public String price;

        public ExchangeRate toExchangeRate() {
            return new ExchangeRate(getCurrency(), getPrice(), new Date(), PROVIDER_NAME);
        }

        public BigDecimal getPrice() {
            return new BigDecimal(price);
        }

        public String getCurrency() {
            // DASH_BTC, DOGE_BTC, LTC_BTC, ...
            return symbol.split("_")[0];
        }

        private boolean isBtcPair() {
            return symbol.endsWith("_BTC");
        }

        public boolean isSupportedCurrency() {
            return isBtcPair() && SUPPORTED_CURRENCIES.contains(getCurrency());
        }
    }
}
