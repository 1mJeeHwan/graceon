package org.streamhub.api.v1.pub.campaign.dto;

import java.time.LocalDateTime;

/**
 * One row of the public campaign list (user site). Only publicly visible campaigns are exposed;
 * {@code summary} is a short, truncated lead derived from the campaign description.
 *
 * @param id       campaign id
 * @param title    campaign title
 * @param summary  short lead text (truncated description)
 * @param imageUrl resolved banner image URL (nullable)
 * @param status   lifecycle status name (e.g. {@code ACTIVE})
 * @param startAt  campaign start time
 * @param endAt    campaign end time
 */
public record CampaignListItem(
        Long id,
        String title,
        String summary,
        String imageUrl,
        String status,
        LocalDateTime startAt,
        LocalDateTime endAt) {
}
