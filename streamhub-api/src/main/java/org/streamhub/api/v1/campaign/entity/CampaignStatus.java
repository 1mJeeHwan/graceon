package org.streamhub.api.v1.campaign.entity;

/**
 * Campaign lifecycle state (C-campaign). Stored via {@code @Enumerated(STRING)}.
 */
public enum CampaignStatus {
    /** 임시저장. */
    DRAFT,
    /** 진행중. */
    ACTIVE,
    /** 종료. */
    ENDED
}
