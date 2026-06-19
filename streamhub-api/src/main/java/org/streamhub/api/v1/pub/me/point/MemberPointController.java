package org.streamhub.api.v1.pub.me.point;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.jwt.MemberTokenResolver;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.me.point.dto.MemberPointsDto;

/**
 * Member "내 포인트" endpoint under the public ({@code /pub/**}, permitAll) namespace. The member is
 * resolved from the Bearer member token via the shared {@link MemberTokenResolver} (never the admin
 * SecurityContext, which ignores member tokens); a missing/invalid member token is a 401.
 */
@Tag(name = "Member Points", description = "사용자 사이트 내 포인트 (회원): 잔액 + 포인트 내역")
@RestController
@RequestMapping("/pub/v1/me/points")
public class MemberPointController {

    private final MemberPointService memberPointService;
    private final MemberTokenResolver memberTokenResolver;

    public MemberPointController(MemberPointService memberPointService,
                                 MemberTokenResolver memberTokenResolver) {
        this.memberPointService = memberPointService;
        this.memberTokenResolver = memberTokenResolver;
    }

    @Operation(summary = "내 포인트", description = "로그인 회원의 현재 포인트 잔액과 포인트 내역(최신순 페이징)을 반환한다.")
    @GetMapping
    public ResultDTO<MemberPointsDto> points(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Long memberId = memberTokenResolver.resolve(authorization);
        return ResultDTO.ok(memberPointService.points(memberId, pageNumber, pageSize));
    }
}
