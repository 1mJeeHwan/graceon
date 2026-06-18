package org.streamhub.api.v1.analytics.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.analytics.entity.ContentKind;

/**
 * Per-content view statistics, grouped by {@code (contentType, targetId)}. Sorted by views
 * descending so the frontend can show top (popular) and bottom (underperforming) content.
 *
 * @param contentType  the content kind (VIDEO / ALBUM / POST)
 * @param targetId     the content id
 * @param title        denormalized content title
 * @param views        number of CONTENT_VIEW events for this target
 * @param avgDwellMs   mean dwell time (ms) over views that report one
 * @param lastViewedAt timestamp of the most recent view
 */
public record ContentStatDto(ContentKind contentType, Long targetId, String title, long views,
                             long avgDwellMs, LocalDateTime lastViewedAt) {
}
