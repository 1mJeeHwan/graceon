package org.streamhub.api.v1.donation;

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
import org.streamhub.api.v1.donation.dto.PlanCreateRequest;
import org.streamhub.api.v1.donation.dto.PlanResponse;

/**
 * Membership-plan management endpoints (SYSTEM or CHURCH_MANAGER; delete is SYSTEM-only).
 */
@Tag(name = "SubscriptionPlan", description = "멤버십 플랜 관리")
@RestController
@RequestMapping("/v1/subscription-plan")
@PreAuthorize("hasAuthority('subscription-plan:read')") // SYSTEM-only resource; mutations require subscription-plan:write
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    public SubscriptionPlanController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @Operation(summary = "멤버십 플랜 목록", description = "가격 오름차순 전체 플랜 목록.")
    @GetMapping("/list")
    public ResultDTO<List<PlanResponse>> list() {
        return ResultDTO.ok(subscriptionPlanService.list());
    }

    @Operation(summary = "멤버십 플랜 상세")
    @GetMapping("/{id}")
    public ResultDTO<PlanResponse> detail(@PathVariable Long id) {
        return ResultDTO.ok(subscriptionPlanService.getDetail(id));
    }

    @Operation(summary = "멤버십 플랜 등록")
    @PreAuthorize("hasAuthority('subscription-plan:write')")
    @PostMapping
    public ResultDTO<PlanResponse> create(@Valid @RequestBody PlanCreateRequest request) {
        return ResultDTO.ok(subscriptionPlanService.create(request));
    }

    @Operation(summary = "멤버십 플랜 수정")
    @PreAuthorize("hasAuthority('subscription-plan:write')")
    @PutMapping("/{id}")
    public ResultDTO<PlanResponse> update(
            @PathVariable Long id, @Valid @RequestBody PlanCreateRequest request) {
        return ResultDTO.ok(subscriptionPlanService.update(id, request));
    }

    @Operation(summary = "멤버십 플랜 삭제", description = "파괴적 액션 — SYSTEM 전용.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription-plan:write')")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        subscriptionPlanService.delete(id);
        return ResultDTO.ok();
    }
}
