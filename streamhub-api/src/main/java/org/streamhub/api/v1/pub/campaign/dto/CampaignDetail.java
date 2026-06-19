package org.streamhub.api.v1.pub.campaign.dto;

import java.time.LocalDateTime;

/**
 * Public campaign detail (user site). Carries the full description; {@code summary} remains a short
 * truncated lead for headers.
 *
 * @param id          campaign id
 * @param title       campaign title
 * @param summary     short lead text (truncated description)
 * @param description full campaign description (nullable)
 * @param imageUrl    resolved banner image URL (nullable)
 * @param status      lifecycle status name (e.g. {@code ACTIVE})
 * @param startAt     campaign start time
 * @param endAt       campaign end time
 */
public record CampaignDetail(
        Long id,
        String title,
        String summary,
        String description,
        String imageUrl,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt) {
}
