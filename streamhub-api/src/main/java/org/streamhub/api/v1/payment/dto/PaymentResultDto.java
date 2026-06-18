package org.streamhub.api.v1.payment.dto;

import org.streamhub.api.v1.order.entity.PayStatus;
import org.streamhub.api.v1.payment.adapter.PaymentResult;

/**
 * API response for payment request/approve. Always carries {@code testMode} so the client can
 * render the "실 PG 미연동 — 가짜 승인(테스트)" badge (정직 표기, spec §7).
 *
 * @param orderId   order id
 * @param provider  PG code used
 * @param status    resulting payment status
 * @param txnId     transaction id
 * @param amount    charged amount (null on request, server total on approval)
 * @param memo      human-readable note
 * @param testMode  always true in the demo (mirrors {@code app.payment.test-mode})
 */
public record PaymentResultDto(
        Long orderId,
        String provider,
        PayStatus status,
        String txnId,
        Long amount,
        String memo,
        String redirectUrl,
        boolean testMode) {

    /** Maps an adapter {@link PaymentResult} to the API DTO. */
    public static PaymentResultDto of(Long orderId, PaymentResult result, boolean testMode) {
        return new PaymentResultDto(
                orderId,
                result.provider(),
                result.status(),
                result.txnId(),
                result.amount(),
                result.memo(),
                result.redirectUrl(),
                testMode);
    }
}
