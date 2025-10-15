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

package bisq.price.common.config;

// Borrowed from bisq.common.config.Config to avoid dependency on bisq.common
public class Config {

    public static final String LEGACY_FEE_DATAMAP = "dataMap";
    public static final String BTC_FEE_INFO = "bitcoinFeeInfo";
    public static final String BTC_FEES_TS = "bitcoinFeesTs";
    public static final String BTC_TX_FEE = "btcTxFee";
    public static final String BTC_MIN_TX_FEE = "btcMinTxFee";
}
