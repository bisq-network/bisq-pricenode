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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.util.Map;

import static bisq.price.spot.RecordedExchangeSupport.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class KrakenTest extends AbstractExchangeRateProviderTest {

    @Test
    public void doGet_successfulCall() {
        doGet_successfulCall(new Kraken(new StandardEnvironment()));
    }

    @Test
    public void doGet_parsesRecordedApiResponses() throws IOException {
        // Responses of the public Kraken API, recorded on 2026-11-27 and trimmed to three
        // pairs. Kraken sends an empty fee list for every pair, which XChange has to accept.
        try (RecordedExchangeSupport api = new RecordedExchangeSupport(Map.of(
                "/0/public/AssetPairs", resource("recorded/kraken/asset-pairs.json"),
                "/0/public/Assets", resource("recorded/kraken/assets.json"),
                "/0/public/Ticker", resource("recorded/kraken/ticker.json")))) {
            assertEquals(Map.of("EUR", 54588.1, "USD", 59691.8, "XMR", 0.006746),
                    api.doGet(new Kraken(new StandardEnvironment())));
        }
    }

}
