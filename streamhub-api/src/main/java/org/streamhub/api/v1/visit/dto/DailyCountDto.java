package org.streamhub.api.v1.visit.dto;

import java.time.LocalDate;

/**
 * One day's visit count for the daily-trend chart.
 *
 * @param date  the calendar day
 * @param count number of visits on that day
 */
public record DailyCountDto(LocalDate date, long count) {
}
