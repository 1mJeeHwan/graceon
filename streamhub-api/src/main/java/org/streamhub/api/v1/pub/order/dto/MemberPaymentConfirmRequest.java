package org.streamhub.api.v1.pub.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Phase 2 of a real-PG album purchase: the values the payment window redirects back with. The
 * server re-checks {@code amount} against the prepared order before calling the PG confirm API,
 * so a tampered redirect cannot change the charged amount.
 *
 * @param orderNo    business order number (the window's {@code orderId})
 * @param paymentKey PG transaction key issued by the window
 * @param amount     amount reported by the window (re-verified against the order total)
 */
public record MemberPaymentConfirmRequest(
        @NotBlank(message = "주문번호가 없습니다") String orderNo,
        @NotBlank(message = "결제키가 없습니다") String paymentKey,
        @NotNull(message = "금액이 없습니다") Long amount) {
}
