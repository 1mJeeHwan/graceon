package org.streamhub.api.v1.pub.campaign;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.campaign.dto.CampaignDetail;
import org.streamhub.api.v1.pub.campaign.dto.CampaignListItem;

/**
 * Public, unauthenticated campaign read API consumed by the user site. Mapped under {@code /pub/**},
 * which is permitAll in {@code SecurityConfig}, so no auth wiring is needed. Exposes only publicly
 * visible (진행중/ACTIVE) campaigns.
 */
@Tag(name = "Public Campaign", description = "사용자 사이트용 공개 캠페인 API (인증 불필요, 진행중만)")
@RestController
@RequestMapping("/pub/v1/campaigns")
public class PublicCampaignController {

    private final PublicCampaignService publicCampaignService;

    public PublicCampaignController(PublicCampaignService publicCampaignService) {
        this.publicCampaignService = publicCampaignService;
    }

    @Operation(summary = "공개 캠페인 목록", description = "공개 노출 대상(진행중) 캠페인을 시작일 최신순으로 페이징해 반환한다.")
    @GetMapping
    public ResultDTO<ResInfinityList<CampaignListItem>> list(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "12") int pageSize) {
        return ResultDTO.ok(publicCampaignService.list(pageNumber, pageSize));
    }

    @Operation(summary = "공개 캠페인 상세", description = "공개 노출 대상 캠페인의 상세를 반환한다. 없거나 비공개면 NOT_FOUND.")
    @GetMapping("/{id}")
    public ResultDTO<CampaignDetail> detail(@PathVariable Long id) {
        return ResultDTO.ok(publicCampaignService.detail(id));
    }
}
