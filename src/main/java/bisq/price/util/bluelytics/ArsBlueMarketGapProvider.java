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

import bisq.price.PriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

@Slf4j
@Component
public class ArsBlueMarketGapProvider extends PriceProvider<OptionalDouble> {
    public interface Listener {
        void onUpdate(OptionalDouble sellGapMultiplier);
    }

    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final BlueLyticsApi blueLyticsApi = new BlueLyticsApi();
    private final Optional<Listener> onUpdateListener;

    public ArsBlueMarketGapProvider() {
        super(REFRESH_INTERVAL);
        this.onUpdateListener = Optional.empty();
    }

    public ArsBlueMarketGapProvider(Listener onUpdateListener) {
        super(REFRESH_INTERVAL);
        this.onUpdateListener = Optional.of(onUpdateListener);
    }

    @Override
    protected OptionalDouble doGet() {
        OptionalDouble sellGapMultiplier = blueLyticsApi.getSellGapMultiplier();
        onUpdateListener.ifPresent(listener -> listener.onUpdate(sellGapMultiplier));
        return sellGapMultiplier;
    }
}
