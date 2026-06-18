package org.streamhub.api.v1.campaign.dto;

import org.streamhub.api.v1.campaign.entity.CampaignStatus;
import org.streamhub.api.v1.campaign.entity.CampaignType;

/** Campaign list search request. All filters optional. */
public record CampaignSearchRequest(
        CampaignType type,
        CampaignStatus status) {
}
