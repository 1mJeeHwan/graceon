package org.streamhub.api.v1.pub.order;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.v1.delivery.adapter.Tracking;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.jwt.JwtTokenProvider;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.order.dto.MemberOrderCreateRequest;
import org.streamhub.api.v1.pub.order.dto.MemberOrderListItem;
import org.streamhub.api.v1.pub.order.dto.MemberOrderResult;
import org.streamhub.api.v1.pub.order.dto.MemberPaymentConfirmRequest;
import org.streamhub.api.v1.pub.order.dto.MemberPaymentPrepareRequest;
import org.streamhub.api.v1.pub.order.dto.MemberPaymentPrepareResult;

/**
 * Member-authenticated album purchase under the public ({@code /pub/**}, permitAll) namespace.
 * Like {@link org.streamhub.api.v1.pub.auth.MemberAuthController}, the member is resolved by
 * parsing the Bearer member token directly — it never relies on the admin SecurityContext, which
 * deliberately ignores member tokens.
 */
@Tag(name = "Member Order", description = "사용자 사이트 음반 구매 (회원)")
@RestController
@RequestMapping("/pub/v1/orders")
public class MemberOrderController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final MemberOrderService memberOrderService;
    private final JwtTokenProvider tokenProvider;

    public MemberOrderController(MemberOrderService memberOrderService, JwtTokenProvider tokenProvider) {
        this.memberOrderService = memberOrderService;
        this.tokenProvider = tokenProvider;
    }

    @Operation(summary = "음반 구매", description = "로그인 회원이 앨범을 구매하면 주문이 생성되고 (가짜) 결제로 PAID 처리된다.")
    @PostMapping
    public ResultDTO<MemberOrderResult> purchase(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody MemberOrderCreateRequest request) {
        return ResultDTO.ok(memberOrderService.purchase(resolveMemberId(authorization), request));
    }

    @Operation(summary = "결제 준비(실 PG)",
            description = "결제창을 열기 전에 주문을 생성하고 결제창에 넘길 orderNo/금액/클라이언트키를 발급한다.")
    @PostMapping("/prepare")
    public ResultDTO<MemberPaymentPrepareResult> prepare(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody MemberPaymentPrepareRequest request) {
        return ResultDTO.ok(memberOrderService.prepare(resolveMemberId(authorization), request));
    }

    @Operation(summary = "결제 승인(실 PG)",
            description = "결제창이 돌려준 paymentKey/orderId/amount로 실제 PG 승인을 호출하고 주문을 PAID로 전이한다.")
    @PostMapping("/confirm")
    public ResultDTO<MemberOrderResult> confirm(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody MemberPaymentConfirmRequest request) {
        return ResultDTO.ok(memberOrderService.confirm(resolveMemberId(authorization), request));
    }

    @Operation(summary = "구매내역", description = "로그인 회원의 주문 목록을 최신순으로 반환한다.")
    @GetMapping
    public ResultDTO<List<MemberOrderListItem>> myOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResultDTO.ok(memberOrderService.myOrders(resolveMemberId(authorization)));
    }

    @Operation(summary = "배송 조회", description = "회원 본인 주문의 운송장으로 실시간 배송 진행상황을 조회한다.")
    @GetMapping("/{orderNo}/tracking")
    public ResultDTO<Tracking> tracking(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNo) {
        return ResultDTO.ok(memberOrderService.trackMyOrder(resolveMemberId(authorization), orderNo));
    }

    private Long resolveMemberId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }
        DecodedJWT jwt = tokenProvider.verify(authorization.substring(BEARER_PREFIX.length()));
        if (!tokenProvider.isMemberToken(jwt)) {
            throw new ApiException(ResultCode.INVALID_TOKEN);
        }
        return Long.valueOf(jwt.getSubject());
    }
}
