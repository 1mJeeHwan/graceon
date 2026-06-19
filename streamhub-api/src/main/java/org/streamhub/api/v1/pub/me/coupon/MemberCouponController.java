package org.streamhub.api.v1.pub.me.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.jwt.MemberTokenResolver;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.me.coupon.dto.MyCouponItem;

/**
 * Member "쿠폰함" endpoint under the public ({@code /pub/**}, permitAll) namespace. The member is
 * resolved from the Bearer member token via the shared {@link MemberTokenResolver} (never the admin
 * SecurityContext, which ignores member tokens); a missing/invalid member token is a 401.
 */
@Tag(name = "Member Coupons", description = "사용자 사이트 쿠폰함 (회원): 사용 가능한 쿠폰 + 사용 여부")
@RestController
@RequestMapping("/pub/v1/me/coupons")
public class MemberCouponController {

    private final MemberCouponService memberCouponService;
    private final MemberTokenResolver memberTokenResolver;

    public MemberCouponController(MemberCouponService memberCouponService,
                                  MemberTokenResolver memberTokenResolver) {
        this.memberCouponService = memberCouponService;
        this.memberTokenResolver = memberTokenResolver;
    }

    @Operation(summary = "쿠폰함", description = "현재 사용 가능한(활성·기간 내) 쿠폰 목록을 회원의 사용 여부와 함께 반환한다.")
    @GetMapping
    public ResultDTO<List<MyCouponItem>> coupons(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long memberId = memberTokenResolver.resolve(authorization);
        return ResultDTO.ok(memberCouponService.coupons(memberId));
    }
}
