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

import bisq.price.PriceProvider;

import java.time.Duration;

/**
 * Abstract base class for providers of mining {@link FeeRate} data.
 */
public abstract class FeeRateProvider extends PriceProvider<FeeRate> {

    public static final long MIN_FEE_RATE_FOR_WITHDRAWAL = 1; // satoshi/vbyte
    public static final long MIN_FEE_RATE_FOR_TRADING = 10; // satoshi/vbyte
    // The cap of Bisq 1 (FeeService.BTC_MAX_TX_FEE from v1.10.1), so versions up to v1.10.0, which have no cap
    // of their own, use the same maximum.
    public static final long MAX_FEE_RATE = 600;

    public FeeRateProvider(Duration refreshInterval) {
        super(refreshInterval);
    }
}
