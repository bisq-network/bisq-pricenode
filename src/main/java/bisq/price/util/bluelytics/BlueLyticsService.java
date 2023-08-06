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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Util singleton object to update and provider ARS/USD blue market gap against the oficial rate.
 * This is useful for example, to estimate ARS/BTC blue (real) market rate in a country with heavy currency controls
 */
public final class BlueLyticsService {
    private static final long MIN_REFRESH_WINDOW = 3600000; // 1hr
    private static final String GET_USD_EXCHANGE_RATES_ARG_URL = "https://api.bluelytics.com.ar/v2/latest";
    private static final BlueLyticsService instance = new BlueLyticsService();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RestTemplate restTemplate = new RestTemplate();
    private Double lastBlueGap;
    private Long lastRefresh;
    private Thread refreshJob;

    private BlueLyticsService() {
        lastRefresh = null;
        lastBlueGap = null;
        refreshJob = null;
    }

    public static BlueLyticsService getInstance() {
        return BlueLyticsService.instance;
    }

    /**
     *
     * @return current ARS/USD gap multiplier to get from official rate to free market rate.
     * If not available returns Nan
     */
    public Double blueGapMultiplier() {
        maybeLaunchAsyncRefresh();
        return Objects.requireNonNullElse(lastBlueGap, Double.NaN);
    }

    /**
     * if enough time {@see BlueLyticsUSDRate.MIN_FRESH_WINDOW} has pass from the last refresh or
     * no refresh has been done before: launch async refresh
     */
    private void maybeLaunchAsyncRefresh() {
        long nowTimestamp = Date.from(Instant.now()).getTime();
        if (refreshJob != null) {
            logger.info("Skipping ARS/USD multiplier refresh since its already running");
        } else {
            if (lastRefresh == null ||
                    nowTimestamp > (lastRefresh + BlueLyticsService.MIN_REFRESH_WINDOW)) {
                if (lastRefresh == null) {
                    logger.info("Refreshing for the first time");
                } else {
                    logger.info(String.format("should refresh? %s with last refresh %s and now time %s", (lastRefresh + BlueLyticsService.MIN_REFRESH_WINDOW), lastRefresh, nowTimestamp));
                }
                launchAsyncRefresh();
            }
        }
    }

    private synchronized void launchAsyncRefresh() {
        logger.info("Launching async refresh of blue ARS/USD rate");
        refreshJob = new Thread(this::refreshBlueGap);
        refreshJob.start();
    }

    private void refreshBlueGap() {
        try {
            // the last_update value is different than the last one and also launch the update if 1 hour passed ?
            lastBlueGap = Objects.requireNonNull(restTemplate.exchange(
                    RequestEntity
                            .get(UriComponentsBuilder
                                    .fromUriString(BlueLyticsService.GET_USD_EXCHANGE_RATES_ARG_URL).build()
                                    .toUri())
                            .build(),
                    new ParameterizedTypeReference<BlueLyticsDto>() {
                    }
            ).getBody()).gapSellMultiplier();
            lastRefresh = new Date().getTime();
            logger.info(String.format("New blue gap is %s and refresh was at epoch %s", lastBlueGap, lastRefresh));
        } catch (Exception e) {
            logger.error("Failed to fetch updated bluelytics gap multiplier", e);
        } finally {
            refreshJob = null;
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cannot clone Singleton");
    }
}
