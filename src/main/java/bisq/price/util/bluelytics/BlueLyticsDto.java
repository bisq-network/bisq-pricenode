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
import java.util.OptionalDouble;

@Getter
@Setter
public class BlueLyticsDto {
    @Getter
    @Setter
    public static class USDRate {
        double value_avg;
        double value_sell;
        double value_buy;
    }

    private BlueLyticsDto.USDRate oficial;
    private BlueLyticsDto.USDRate blue;
    private Date last_update;

    public OptionalDouble gapSellMultiplier() {
        double sellMultiplier = blue.value_sell / oficial.value_sell;
        return Double.isNaN(sellMultiplier) ? OptionalDouble.empty() : OptionalDouble.of(sellMultiplier);
    }
}
