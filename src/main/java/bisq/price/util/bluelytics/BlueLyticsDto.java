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


package bisq.price.util.bluelytics;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class BlueLyticsDto {
    @Getter
    @Setter
    public static class USDRate {
        Double value_avg;
        Double value_sell;
        Double value_buy;
    }

    BlueLyticsDto.USDRate oficial;
    BlueLyticsDto.USDRate blue;
    Date last_update;

    /**
     *
     * @return the sell multiplier to go from oficial to blue market for ARS/USD
     *  if its not available, returns NaN
     */
    public Double gapSellMultiplier() {
        return this.blue.value_sell / this.oficial.value_sell;
    }
}
