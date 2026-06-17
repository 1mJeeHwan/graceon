package org.streamhub.api.v1.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.payment.dto.PayApproveCommand;
import org.streamhub.api.v1.payment.dto.PayRequestCommand;
import org.streamhub.api.v1.payment.dto.PaymentReceiptDto;
import org.streamhub.api.v1.payment.dto.PaymentResultDto;

/**
 * Payment endpoints (SYSTEM or CHURCH_MANAGER). All approvals are demo/test mode — no real PG
 * call is made and {@code testMode=true} is always returned (C4).
 */
@Tag(name = "Payment", description = "결제 요청·승인·영수증 (데모/테스트 모드, 실 PG 미연동)")
@RestController
@RequestMapping("/v1/payment")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "결제 요청", description = "주문에 대해 (가짜) 결제를 요청하고 거래번호를 발급한다.")
    @PostMapping("/request")
    public ResultDTO<PaymentResultDto> request(@Valid @RequestBody PayRequestCommand request) {
        return ResultDTO.ok(paymentService.request(request));
    }

    @Operation(summary = "결제 승인",
            description = "요청된 결제를 (가짜) 승인하고 주문을 PAID로 전이 + 영수증을 발급한다.")
    @PostMapping("/approve")
    public ResultDTO<PaymentResultDto> approve(@Valid @RequestBody PayApproveCommand request) {
        return ResultDTO.ok(paymentService.approve(request));
    }

    @Operation(summary = "결제 영수증", description = "주문의 최신 결제(PAY) 영수증을 반환한다.")
    @GetMapping("/{orderId}/receipt")
    public ResultDTO<PaymentReceiptDto> receipt(@PathVariable Long orderId) {
        return ResultDTO.ok(paymentService.receipt(orderId));
    }
}
