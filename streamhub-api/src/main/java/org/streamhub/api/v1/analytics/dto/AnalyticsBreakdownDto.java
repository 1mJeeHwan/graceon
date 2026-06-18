package org.streamhub.api.v1.analytics.dto;

import java.util.List;
import java.util.Map;
import org.streamhub.api.v1.analytics.entity.DeviceKind;

/**
 * Categorical breakdown of web-analytics traffic.
 *
 * @param byDevice     event counts keyed by {@link DeviceKind} (PC / MOBILE / TABLET)
 * @param topReferrers six most-frequent referrers, descending
 * @param topPaths     eight most-visited paths, descending
 */
public record AnalyticsBreakdownDto(Map<DeviceKind, Long> byDevice, List<CountItemDto> topReferrers,
                                    List<CountItemDto> topPaths) {
}
