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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
class TradeOgre extends ExchangeRateProvider {
    private static final String TRADEOGRE_URL = "https://tradeogre.com/api/v1/ticker/";
    public TradeOgre(Environment env) {
        super(env, "TRADEOGRE", "tradeogre", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {
        Set<ExchangeRate> exchangeRateSet = new HashSet<>();
        exchangeRateSet.add(getMarketData("XMR"));
        return exchangeRateSet;
    }

    private ExchangeRate getMarketData(String ccy) {
        ResponseEntity<TradeOgreTicker> response =
                new CustomRestTemplate().getForEntity(
                        UriComponentsBuilder
                                .fromUriString(TradeOgre.TRADEOGRE_URL + ccy + "-BTC").build()
                                .toUri(),
                        TradeOgreTicker.class);
        TradeOgreTicker ticker = response.getBody();
        return new ExchangeRate(ccy, new BigDecimal(ticker.price), new Date(), this.getName());
    }

    // we need a custom RestTemplate due to TradeOgre not setting http contentType=json in their feed
    private static class CustomRestTemplate extends RestTemplate {
        @Override
        protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback,
                                  ResponseExtractor<T> responseExtractor) throws RestClientException {
            ClientHttpResponse response = null;
            try {
                ClientHttpRequest request = createRequest(url, method);
                if (requestCallback != null) {
                    requestCallback.doWithRequest(request);
                }
                response = request.execute();
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                if (getErrorHandler().hasError(response)) {
                    getErrorHandler().handleError(response);
                }
                return responseExtractor == null ? null : responseExtractor.extractData(response);
            }
            catch (IOException ex) {
                throw new ResourceAccessException("I/O error on " + method.name() +
                        " request for \"" + url + "\":" + ex.getMessage(), ex);
            }
            finally {
                if (response != null) {
                    response.close();
                }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TradeOgreTicker {
        public String price;
    }
}
