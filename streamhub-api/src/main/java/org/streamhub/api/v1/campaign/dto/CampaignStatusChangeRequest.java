package org.streamhub.api.v1.campaign.dto;

import jakarta.validation.constraints.NotNull;
import org.streamhub.api.v1.campaign.entity.CampaignStatus;

/** Request to transition a campaign to a new {@code status}. */
public record CampaignStatusChangeRequest(
        @NotNull CampaignStatus status) {
}
