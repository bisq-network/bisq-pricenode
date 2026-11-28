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

import bisq.price.AbstractExchangeRateProviderTest;
import bisq.price.spot.RecordedExchangeSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static bisq.price.spot.RecordedExchangeSupport.readResource;
import static bisq.price.spot.RecordedExchangeSupport.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class BitfinexTest extends AbstractExchangeRateProviderTest {

    @Test
    public void doGet_successfulCall() {
        doGet_successfulCall(new Bitfinex(new StandardEnvironment()));
    }

    @Test
    public void doGet_parsesRecordedApiResponses() throws IOException {
        // Responses of the public Bitfinex API, recorded on 2026-11-27 and trimmed to six
        // spot pairs, three perpetual swaps and one funding currency. Bitfinex sends 12
        // values per trading pair ticker and 18 per funding ticker, and XChange has to
        // ignore the values it does not map. The swaps must not add a second rate for
        // BTC/USDT, ETH or LTC.
        try (RecordedExchangeSupport api = new RecordedExchangeSupport(Map.of(
                "/v2/conf/pub:map:currency:undl", resource("recorded/bitfinex/v2-conf-pub-map-currency-undl.json"),
                "/v2/conf/pub:info:pair", resource("recorded/bitfinex/v2-conf-pub-info-pair.json"),
                "/v2/tickers", BitfinexTest::recordedTickers))) {
            assertEquals(Map.of("EUR", 54892.0, "USD", 59768.0, "USDT-E", 1.673E-5,
                            "ETH", 0.02788, "LTC", 7.3909E-4, "XMR", 0.0067026),
                    api.doGet(new Bitfinex(new StandardEnvironment())));
        }
    }

    /**
     * Like Bitfinex, answers the tickers of the requested symbols, or all of them for ALL.
     */
    private static byte[] recordedTickers(URI uri) {
        Set<String> symbols = Set.copyOf(Arrays.asList(uri.getQuery().substring("symbols=".length()).split(",")));
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode recorded = (ArrayNode) mapper.readTree(readResource("recorded/bitfinex/v2-tickers.json"));
            ArrayNode answer = mapper.createArrayNode();
            recorded.forEach(ticker -> {
                if (symbols.contains("ALL") || symbols.contains(ticker.get(0).asText())) {
                    answer.add(ticker);
                }
            });
            return mapper.writeValueAsBytes(answer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
