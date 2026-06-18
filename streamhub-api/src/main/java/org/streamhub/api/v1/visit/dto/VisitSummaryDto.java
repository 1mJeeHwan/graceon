package org.streamhub.api.v1.visit.dto;

import java.util.List;
import java.util.Map;
import org.streamhub.api.v1.visit.entity.DeviceType;

/**
 * Dashboard summary of front-site traffic.
 *
 * @param totalVisits     all-time visit count
 * @param todayVisits     visits recorded today
 * @param uniqueIpApprox  approximate unique-visitor count (distinct masked IPs)
 * @param topPaths        five most-visited paths, descending
 * @param deviceBreakdown visit counts keyed by {@link DeviceType} (PC / MOBILE / TABLET)
 */
public record VisitSummaryDto(long totalVisits, long todayVisits, long uniqueIpApprox,
                              List<PathCountDto> topPaths, Map<DeviceType, Long> deviceBreakdown) {
}
