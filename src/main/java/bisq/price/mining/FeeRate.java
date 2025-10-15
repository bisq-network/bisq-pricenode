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

package bisq.price.mining;

import lombok.Getter;

/**
 * A value object representing the mining fee rate for a given base currency.
 */
@Getter
public class FeeRate {

    private final String currency;
    private final long price;
    private final long minimumFee;
    private final long timestamp;

    public FeeRate(String currency, long price, long minimumFee, long timestamp) {
        this.currency = currency;
        this.price = price;
        this.minimumFee = minimumFee;
        this.timestamp = timestamp;
    }

}
