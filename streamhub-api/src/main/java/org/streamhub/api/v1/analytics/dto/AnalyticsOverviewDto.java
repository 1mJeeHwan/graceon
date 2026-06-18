package org.streamhub.api.v1.analytics.dto;

/**
 * Dashboard overview of web-analytics traffic.
 *
 * @param totalEvents    all-time event count
 * @param totalSessions  distinct session ids
 * @param uniqueVisitors distinct member ids (nulls excluded)
 * @param pageViews      number of PAGE_VIEW events
 * @param contentViews   number of CONTENT_VIEW events
 * @param avgDwellMs     mean dwell time (ms) over events that report one
 */
public record AnalyticsOverviewDto(long totalEvents, long totalSessions, long uniqueVisitors,
                                   long pageViews, long contentViews, long avgDwellMs) {
}
