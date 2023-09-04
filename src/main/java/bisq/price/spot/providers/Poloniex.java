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

import java.math.BigDecimal;
import java.time.Duration;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
class Poloniex extends ExchangeRateProvider {
    // Supported fiat: -
    // Supported alts: DASH, DCR, DOGE, ETC, ETH, LTC, XMR, ZEC
    private static final String CURRENCIES = "DASH,DCR,DOGE,ETC,ETH,LTC,XMR,ZEC";
    private static final String POLONIEX_URL = "https://api.poloniex.com/markets/price";
    public Poloniex(Environment env) {
        super(env, "POLO", "poloniex", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {
        Set<String> requestedCurrencies = new HashSet<>(Arrays.asList(CURRENCIES.split(",")));
        Set<ExchangeRate> exchangeRates = new HashSet<>();
        PoloniexTicker[] tickers =
                WebClient.create().get()
                    .uri(POLONIEX_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PoloniexTicker[].class)
                .block(Duration.of(30, ChronoUnit.SECONDS));
        Arrays.stream(Objects.requireNonNull(tickers))
                .filter(ticker -> ticker.isMatch(requestedCurrencies))
                .forEach(ticker -> exchangeRates.add(
                        new ExchangeRate(ticker.getCurrency(), ticker.getPrice(), new Date(), this.getName())));
        return exchangeRates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PoloniexTicker {
        public String symbol;
        public String price;

        public BigDecimal getPrice() {
            return new BigDecimal(price);
        }

        public String getCurrency() {
            // symbol might not be a BTC pair, in which case return empty string
            int splitIndex = symbol.indexOf("_BTC");
            if (splitIndex < 0) {
                return "";
            }
            return symbol.substring(0, splitIndex);
        }

        public boolean isMatch(Set<String> requestedCurrencies) {
            return symbol.equalsIgnoreCase(getCurrency() + "_BTC") &&
                    requestedCurrencies.contains(getCurrency());
        }
    }
}
