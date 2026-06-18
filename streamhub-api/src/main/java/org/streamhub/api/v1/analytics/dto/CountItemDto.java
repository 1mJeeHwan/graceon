package org.streamhub.api.v1.analytics.dto;

/**
 * A label and its count, used for the referrer / path breakdowns.
 *
 * @param label the referrer source or path
 * @param count number of events for that label
 */
public record CountItemDto(String label, long count) {
}
