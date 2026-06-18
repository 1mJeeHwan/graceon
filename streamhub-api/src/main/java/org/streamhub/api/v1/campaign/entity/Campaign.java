package org.streamhub.api.v1.campaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A campaign/event (C-campaign): special donations, new-release pre-orders, events and
 * seasonal promotions. Optionally links to goods and carries a fundraising target. All
 * values are demo/fictional (no real business data — PII guard).
 */
@Entity
@Table(name = "CAMPAIGN", indexes = {
        @Index(name = "idx_campaign_type", columnList = "type"),
        @Index(name = "idx_campaign_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private CampaignType type;

    @Column(name = "description", length = 1000, columnDefinition = "TEXT")
    private String description;

    /** Banner image URL (demo picsum); nullable. */
    @Column(name = "banner_image_url", length = 300)
    private String bannerImageUrl;

    /** Comma-separated linked goods ids (e.g. {@code "1,2,3"}); nullable. */
    @Column(name = "linked_goods_ids", length = 300)
    private String linkedGoodsIds;

    /** Fundraising target amount for donation campaigns; nullable. */
    @Column(name = "target_amount")
    private Long targetAmount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CampaignStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Campaign(String title, CampaignType type, String description, String bannerImageUrl,
                     String linkedGoodsIds, Long targetAmount, LocalDateTime startAt,
                     LocalDateTime endAt, CampaignStatus status, LocalDateTime createdAt) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.bannerImageUrl = bannerImageUrl;
        this.linkedGoodsIds = linkedGoodsIds;
        this.targetAmount = targetAmount;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status != null ? status : CampaignStatus.DRAFT;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** Updates editable fields. */
    public void update(String title, CampaignType type, String description, String bannerImageUrl,
                       String linkedGoodsIds, Long targetAmount, LocalDateTime startAt,
                       LocalDateTime endAt, CampaignStatus status) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.bannerImageUrl = bannerImageUrl;
        this.linkedGoodsIds = linkedGoodsIds;
        this.targetAmount = targetAmount;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

    /** Transitions the campaign to a new lifecycle state. */
    public void changeStatus(CampaignStatus status) {
        this.status = status;
    }
}
