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

package bisq.price.util.cryptoya;

import bisq.price.spot.ExchangeRate;
import bisq.price.spot.providers.CryptoYa;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Optional;

@Getter
@Setter
public class CryptoYaTicker {

    private double ask;
    private double totalAsk;

    private double bid;
    private double totalBid;

    private long time;

    public Optional<ExchangeRate> toExchangeRate(String exchangeName, Instant newerThan) {
        if (!CryptoYa.EXCHANGE_NAME_WHITELIST.contains(exchangeName) || isTooOld(newerThan) || isAskZeroOrNegative()) {
            return Optional.empty();
        }

        return Optional.of(
                new ExchangeRate(
                        "ARS",
                        ask,
                        System.currentTimeMillis(),
                        CryptoYa.PROVIDER_NAME + ": " + exchangeName
                )
        );
    }

    private boolean isTooOld(Instant newerThan) {
        return time <= newerThan.getEpochSecond();
    }

    private boolean isAskZeroOrNegative() {
        return ask <= 0;
    }
}
