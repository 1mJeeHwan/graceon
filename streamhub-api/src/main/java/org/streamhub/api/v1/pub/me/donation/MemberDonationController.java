package org.streamhub.api.v1.pub.me.donation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.jwt.MemberTokenResolver;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.me.donation.dto.MyDonationItem;

/**
 * Member recurring-donation endpoints under the public ({@code /pub/**}, permitAll) namespace.
 * The member is resolved from the Bearer member token via the shared {@link MemberTokenResolver}
 * (a missing/invalid member token is a 401). Every result is scoped to the resolved member.
 */
@Tag(name = "Member Donations", description = "사용자 사이트 정기후원 (회원): 내 정기후원/구독 목록")
@RestController
@RequestMapping("/pub/v1/me/donations")
public class MemberDonationController {

    private final MemberDonationService donationService;
    private final MemberTokenResolver memberTokenResolver;

    public MemberDonationController(MemberDonationService donationService,
                                    MemberTokenResolver memberTokenResolver) {
        this.donationService = donationService;
        this.memberTokenResolver = memberTokenResolver;
    }

    @Operation(summary = "내 정기후원 목록",
            description = "로그인 회원의 정기후원/구독을 플랜 정보(이름/금액/주기)와 함께 최신순으로 반환한다.")
    @GetMapping
    public ResultDTO<List<MyDonationItem>> donations(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long memberId = memberTokenResolver.resolve(authorization);
        return ResultDTO.ok(donationService.donations(memberId));
    }
}
