package org.streamhub.api.v1.campaign.entity;

/**
 * Campaign/event category (C-campaign). Stored via {@code @Enumerated(STRING)}.
 */
public enum CampaignType {
    /** 특별헌금. */
    SPECIAL_DONATION,
    /** 신간 출시. */
    NEW_RELEASE,
    /** 이벤트. */
    EVENT,
    /** 시즌 캠페인. */
    SEASONAL
}
