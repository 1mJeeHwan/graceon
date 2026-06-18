package org.streamhub.api.v1.campaign.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.campaign.entity.Campaign;
import org.streamhub.api.v1.campaign.entity.CampaignStatus;
import org.streamhub.api.v1.campaign.entity.CampaignType;

/**
 * A campaign/event row. Used as both the admin create/update input and the list/detail
 * output. All values are demo/fictional (PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class CampaignDto {
    private Long id;
    private String title;
    private CampaignType type;
    private String description;
    private String bannerImageUrl;
    private String linkedGoodsIds;
    private Long targetAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private CampaignStatus status;
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted campaign. */
    public static CampaignDto from(Campaign campaign) {
        CampaignDto dto = new CampaignDto();
        dto.id = campaign.getId();
        dto.title = campaign.getTitle();
        dto.type = campaign.getType();
        dto.description = campaign.getDescription();
        dto.bannerImageUrl = campaign.getBannerImageUrl();
        dto.linkedGoodsIds = campaign.getLinkedGoodsIds();
        dto.targetAmount = campaign.getTargetAmount();
        dto.startAt = campaign.getStartAt();
        dto.endAt = campaign.getEndAt();
        dto.status = campaign.getStatus();
        dto.createdAt = campaign.getCreatedAt();
        return dto;
    }
}
