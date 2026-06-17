package org.streamhub.api.v1.payment.adapter;

import org.streamhub.api.v1.order.entity.PayStatus;

/**
 * Provider-agnostic payment result returned from a {@link PaymentProvider} (C4 payment seam).
 *
 * @param status   resulting {@link PayStatus} ({@code REQUESTED} / {@code APPROVED} / {@code FAILED})
 * @param provider PG code that produced this result
 * @param txnId    transaction id (mock = {@code MOCK-{orderNo}-{seq}}, real = PG paymentKey)
 * @param amount   charged amount (echoed back from the request on approval)
 * @param memo     human-readable note (e.g. {@code "MOCK 승인(실거래 아님)"})
 */
public record PaymentResult(PayStatus status, String provider, String txnId, Long amount, String memo) {

    /** A successful payment request (status {@link PayStatus#REQUESTED}). */
    public static PaymentResult requested(String provider, String txnId) {
        return new PaymentResult(PayStatus.REQUESTED, provider, txnId, null, "결제요청 접수(MOCK)");
    }

    /** A successful payment approval (status {@link PayStatus#APPROVED}). */
    public static PaymentResult approved(String provider, String txnId, Long amount, String memo) {
        return new PaymentResult(PayStatus.APPROVED, provider, txnId, amount, memo);
    }
}
