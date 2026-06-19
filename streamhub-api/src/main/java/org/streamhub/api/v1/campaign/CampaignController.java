package org.streamhub.api.v1.campaign;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.campaign.dto.CampaignDto;
import org.streamhub.api.v1.campaign.dto.CampaignSearchRequest;
import org.streamhub.api.v1.campaign.dto.CampaignStatusChangeRequest;

/**
 * Campaign/event management endpoints (SYSTEM or CHURCH_MANAGER): special donations,
 * new-release pre-orders, events and seasonal promotions.
 */
@Tag(name = "Campaign", description = "캠페인·이벤트 관리")
@RestController
@RequestMapping("/v1/campaign")
@PreAuthorize("hasAuthority('campaign:read')") // class default = read; mutations require campaign:write
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Operation(summary = "캠페인 목록", description = "관리자용 전체 캠페인 목록(최신순). 유형/상태로 필터링.")
    @PostMapping("/list")
    public ResultDTO<List<CampaignDto>> list(@RequestBody(required = false) CampaignSearchRequest request) {
        return ResultDTO.ok(campaignService.list(request));
    }

    @Operation(summary = "캠페인 상세")
    @GetMapping("/{id}")
    public ResultDTO<CampaignDto> detail(@PathVariable Long id) {
        return ResultDTO.ok(campaignService.get(id));
    }

    @Operation(summary = "캠페인 등록")
    @PreAuthorize("hasAuthority('campaign:write')")
    @PostMapping
    public ResultDTO<CampaignDto> create(@Valid @RequestBody CampaignDto request) {
        return ResultDTO.ok(campaignService.create(request));
    }

    @Operation(summary = "캠페인 수정")
    @PreAuthorize("hasAuthority('campaign:write')")
    @PutMapping("/{id}")
    public ResultDTO<CampaignDto> update(@PathVariable Long id, @Valid @RequestBody CampaignDto request) {
        return ResultDTO.ok(campaignService.update(id, request));
    }

    @Operation(summary = "캠페인 상태 변경")
    @PreAuthorize("hasAuthority('campaign:write')")
    @PutMapping("/{id}/status")
    public ResultDTO<CampaignDto> changeStatus(
            @PathVariable Long id, @Valid @RequestBody CampaignStatusChangeRequest request) {
        return ResultDTO.ok(campaignService.changeStatus(id, request.status()));
    }

    @Operation(summary = "캠페인 삭제")
    @PreAuthorize("hasAuthority('campaign:write')")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResultDTO.ok();
    }
}
