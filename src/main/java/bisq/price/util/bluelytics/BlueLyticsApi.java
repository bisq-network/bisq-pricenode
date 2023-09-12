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

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.OptionalDouble;

public class BlueLyticsApi {
    private static final String API_URL = "https://api.bluelytics.com.ar/v2/latest";
    private final WebClient webClient = WebClient.create();

    public OptionalDouble getSellGapMultiplier() {
        BlueLyticsDto blueLyticsDto = webClient.get()
                .uri(API_URL)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BlueLyticsDto.class)
                .block(Duration.of(30, ChronoUnit.SECONDS));

        return blueLyticsDto == null ? OptionalDouble.empty() : blueLyticsDto.gapSellMultiplier();
    }
}
