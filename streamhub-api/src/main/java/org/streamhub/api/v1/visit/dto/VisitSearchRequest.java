package org.streamhub.api.v1.visit.dto;

import java.time.LocalDate;
import org.streamhub.api.v1.visit.entity.DeviceType;

/**
 * Visit-list / daily-aggregate filter. The date range is inclusive; missing bounds fall back to a
 * default recent window in the service. {@code keyword} matches against path / browser / OS.
 *
 * @param fromDate   inclusive range start (nullable → service default)
 * @param toDate     inclusive range end (nullable → today)
 * @param deviceType optional device-class filter
 * @param keyword    optional case-insensitive match on path / browser / OS
 */
public record VisitSearchRequest(LocalDate fromDate, LocalDate toDate, DeviceType deviceType,
                                 String keyword) {
}
