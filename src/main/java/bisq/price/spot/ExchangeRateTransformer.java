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

package bisq.price.spot;

import java.util.Optional;

/**
 * An ExchangeRateTransformer allows to apply a transformation on a particular ExchangeRate
 * for particular supported currencies. This is useful for countries with currency  controls
 * that have a "blue" market in place for real/free trades.
 */
public interface ExchangeRateTransformer {
    Optional<ExchangeRate> apply(ExchangeRateProvider provider, ExchangeRate exchangeRate);

    String supportedCurrency();
}
