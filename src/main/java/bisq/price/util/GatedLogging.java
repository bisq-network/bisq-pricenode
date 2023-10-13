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

package bisq.price.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/* Per https://github.com/bisq-network/bisq-pricenode/issues/33
 * There's too much logging of outlier filtering data, fills up the logs too fast and obliterates other valid logging.
 * It correlates with client requests. Change that logging so its output once per minute.
 */

@Slf4j
public class GatedLogging {
    private long timestampOfLastLogMessage = 0;

    public void maybeLogInfo(String format, Object... params) {
        if (gatingOperation()) {
            log.info(format, params);
        }
    }

    public boolean gatingOperation() {
        if (Instant.now().getEpochSecond() - timestampOfLastLogMessage > 60) {
            timestampOfLastLogMessage = Instant.now().getEpochSecond();
            return true;
        }
        return false;
    }
}
