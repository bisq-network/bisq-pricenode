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

package bisq.price.util.yadio;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class YadioTicker {
    // in USD
    private double BTC;

    // prices of other currencies currencies in USD
    private Map<String, Double> USD;

    // currency used as base for each price in the USD map
    private String base;

    // timestamp on last rates update
    private long timestamp;

    /**
     * @return true if not too old and prices are based in USD, false otherwise
     */
    public boolean isValid(Instant yesterdayInstant) {
        return base.equals("USD") && timestamp > yesterdayInstant.toEpochMilli();
    }
}
