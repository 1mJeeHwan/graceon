package org.streamhub.api.v1.member;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.v1.member.dto.PointGrantRequest;
import org.streamhub.api.v1.member.dto.PointLedgerListItem;
import org.streamhub.api.v1.member.dto.PointLedgerSearchRequest;

/**
 * Grace-point ledger endpoints. Accessible to SYSTEM and CHURCH_MANAGER operators;
 * the service scopes CHURCH_MANAGER access to their own church's members.
 */
@Tag(name = "Point", description = "포인트 원장 관리")
@RestController
@RequestMapping("/v1/point")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @Operation(summary = "포인트 원장 목록", description = "검색/필터/페이지네이션된 포인트 원장 목록을 반환한다.")
    @PostMapping("/list")
    public ResultDTO<ResInfinityList<PointLedgerListItem>> list(
            @RequestBody PointLedgerSearchRequest request,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResultDTO.ok(pointService.list(request, principal));
    }

    @Operation(summary = "회원별 포인트 원장", description = "회원 상세 포인트 탭용. 해당 회원의 원장을 페이지네이션해 반환한다.")
    @GetMapping("/member/{memberId}")
    public ResultDTO<ResInfinityList<PointLedgerListItem>> memberLedger(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResultDTO.ok(pointService.listByMember(memberId, pageNumber, pageSize, principal));
    }

    @Operation(summary = "포인트 수동 지급/차감", description = "원장 기록과 회원 누적 포인트를 한 트랜잭션에서 동기화한다. delta 음수는 차감.")
    @PostMapping("/grant")
    public ResultDTO<PointLedgerListItem> grant(
            @Valid @RequestBody PointGrantRequest request,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResultDTO.ok(pointService.grant(request, principal));
    }
}
