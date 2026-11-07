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
    // Floor of the Bisq 1 trade fee rate. Versions up to v1.10.11 use the delivered rate as it is, so for them
    // this is the only floor; later versions also keep at least FeeService.MIN_TRADE_TX_FEE_RATE (2) themselves.
    // It must not be below that: Bisq 1 makers refuse a trade tx fee below 424 sat (250 sat up to v1.10.11),
    // and takers state at least 212 (API) or 233 (desktop) vbytes times this rate.
    public static final long MIN_FEE_RATE_FOR_TRADING = 2; // satoshi/vbyte
    // The cap of Bisq 1 (FeeService.BTC_MAX_TX_FEE from v1.10.1), so versions up to v1.10.0, which have no cap
    // of their own, use the same maximum.
    public static final long MAX_FEE_RATE = 600;

    public FeeRateProvider(Duration refreshInterval) {
        super(refreshInterval);
    }
}
