package org.streamhub.api.v1.pub.campaign;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.campaign.entity.Campaign;
import org.streamhub.api.v1.campaign.entity.CampaignStatus;
import org.streamhub.api.v1.campaign.repository.CampaignRepository;
import org.streamhub.api.v1.pub.campaign.dto.CampaignDetail;
import org.streamhub.api.v1.pub.campaign.dto.CampaignListItem;

/**
 * Public, unauthenticated campaign read API for the user site. Exposes only publicly visible
 * campaigns (those in the {@link CampaignStatus#ACTIVE} lifecycle state — {@code DRAFT} and
 * {@code ENDED} are hidden), newest-starting first. Banner image keys/URLs are resolved through
 * {@link StorageService#publicUrl} like every other public read endpoint, and the description is
 * truncated into a short {@code summary} lead for list headers.
 */
@Service
public class PublicCampaignService {

    /** Only this lifecycle state is publicly visible (진행중). DRAFT and ENDED are hidden. */
    private static final CampaignStatus PUBLIC_STATUS = CampaignStatus.ACTIVE;
    private static final int SUMMARY_MAX = 120;

    private final CampaignRepository campaignRepository;
    private final StorageService storageService;

    public PublicCampaignService(CampaignRepository campaignRepository, StorageService storageService) {
        this.campaignRepository = campaignRepository;
        this.storageService = storageService;
    }

    /** A page of publicly visible campaigns, newest-starting first. */
    @Transactional(readOnly = true)
    public ResInfinityList<CampaignListItem> list(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNumber), Math.max(1, pageSize));
        Page<Campaign> page =
                campaignRepository.findByStatusOrderByStartAtDescIdDesc(PUBLIC_STATUS, pageable);
        List<CampaignListItem> contents = page.getContent().stream()
                .map(this::toListItem)
                .toList();
        return ResInfinityList.of(contents, page.getTotalElements(), Math.max(1, pageSize));
    }

    /**
     * One publicly visible campaign by id.
     *
     * @throws ApiException {@code NOT_FOUND} if missing or not publicly visible
     */
    @Transactional(readOnly = true)
    public CampaignDetail detail(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .filter(c -> c.getStatus() == PUBLIC_STATUS)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return new CampaignDetail(
                campaign.getId(),
                campaign.getTitle(),
                summary(campaign.getDescription()),
                campaign.getDescription(),
                storageService.publicUrl(campaign.getBannerImageUrl()),
                campaign.getStatus().name(),
                campaign.getStartAt(),
                campaign.getEndAt());
    }

    private CampaignListItem toListItem(Campaign campaign) {
        return new CampaignListItem(
                campaign.getId(),
                campaign.getTitle(),
                summary(campaign.getDescription()),
                storageService.publicUrl(campaign.getBannerImageUrl()),
                campaign.getStatus().name(),
                campaign.getStartAt(),
                campaign.getEndAt());
    }

    /** A short lead derived from the description (trimmed and length-capped), or {@code null}. */
    private String summary(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.strip();
        return trimmed.length() <= SUMMARY_MAX ? trimmed : trimmed.substring(0, SUMMARY_MAX) + "…";
    }
}
