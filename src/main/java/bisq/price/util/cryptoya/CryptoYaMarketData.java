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


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
@Setter
public class CryptoYaMarketData {

    private CryptoYaTicker argenbtc;
    private CryptoYaTicker buenbit;
    private CryptoYaTicker ripio;
    private CryptoYaTicker ripioexchange;
    private CryptoYaTicker satoshitango;
    private CryptoYaTicker cryptomkt;
    private CryptoYaTicker decrypto;
    private CryptoYaTicker latamex;
    private CryptoYaTicker bitso;
    private CryptoYaTicker letsbit;
    private CryptoYaTicker fiwind;
    private CryptoYaTicker lemoncash;
    private CryptoYaTicker bitmonedero;
    private CryptoYaTicker belo;
    private CryptoYaTicker tiendacrypto;
    private CryptoYaTicker saldo;
    private CryptoYaTicker kriptonmarket;
    private CryptoYaTicker calypso;
    private CryptoYaTicker bybit;
    private CryptoYaTicker binance;

    /**
     *
     * @return the avg ask price from all the exchanges that have updated ask prices (not older than 1 day)
     *      if no market data available returns 0
     */
    public Double averagedArsBlueRateFromLast24Hours() {
        // filter more than 1 day old values with yesterday UTC timestamp
        Long yesterdayTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        return streamLatestAvailableMarkets(yesterdayTimestamp).mapToDouble(CryptoYaTicker::getAsk)
                .average()
                .orElse(0.0d);
    }

    private Stream<CryptoYaTicker> streamLatestAvailableMarkets(Long startingTime) {
        return Stream.of(argenbtc, buenbit, ripio, ripioexchange, satoshitango,
                cryptomkt, decrypto, latamex, bitso, letsbit, fiwind,
                lemoncash, bitmonedero, belo, tiendacrypto, saldo,
                kriptonmarket, calypso, bybit, binance)
                    .filter(Objects::nonNull)
                    .filter(rate -> rate.getTime() > startingTime);
    }
}
