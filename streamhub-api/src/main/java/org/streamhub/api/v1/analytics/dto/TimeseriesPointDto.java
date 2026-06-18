package org.streamhub.api.v1.analytics.dto;

import java.time.LocalDate;

/**
 * One day's analytics counts for the trend chart.
 *
 * @param date     the calendar day
 * @param events   number of events on that day
 * @param sessions distinct session ids on that day
 */
public record TimeseriesPointDto(LocalDate date, long events, long sessions) {
}
