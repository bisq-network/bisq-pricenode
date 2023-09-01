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
import bisq.price.util.cryptoya.CryptoYaMarketData;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * CryptoYa is used only for Argentina Peso (ARS).
 * Currency controls in the country forces a black market where the currency can be traded freely.
 * Official and easily available rates cannot be trusted, therefore a specific fetch needs to be done
 * for these scenarios.
 * This ExchangeRateProvider provides a real market rate (black or "blue") for ARS/BTC
 */
@Component
class CryptoYa extends ExchangeRateProvider {

    private static final String CRYPTO_YA_BTC_ARS_API_URL = "https://criptoya.com/api/btc/ars/0.1";

    private final RestTemplate restTemplate = new RestTemplate();

    public CryptoYa(Environment env) {
        super(env, "CRYPTOYA", "cryptoya", Duration.ofMinutes(1));
    }

    /**
     * @return average price buy/sell price averaging different providers suported by cryptoya api
     * which uses the free market (or blue, or unofficial) ARS price for BTC
     */
    @Override
    public Set<ExchangeRate> doGet() {
        Set<ExchangeRate> result = new HashSet<>();
        String key = "ARS";

        OptionalDouble rate = fetchArsBlueMarketData().averagedArsBlueRateFromLast24Hours();
        if (rate.isPresent()) {
            result.add(new ExchangeRate(
                    key,
                    BigDecimal.valueOf(rate.getAsDouble()),
                    new Date(),
                    this.getName()
            ));
        }
        return result;
    }

    private CryptoYaMarketData fetchArsBlueMarketData() {
        return restTemplate.getForObject(CRYPTO_YA_BTC_ARS_API_URL, CryptoYaMarketData.class);
    }
}
