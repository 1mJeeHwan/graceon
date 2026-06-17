package org.streamhub.api.v1.payment.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.order.entity.OrderReceipt;
import org.streamhub.api.v1.order.entity.ReceiptKind;

/**
 * Payment receipt view (C4) — exposes the PG {@code provider}/{@code txnId} backfilled by
 * {@link org.streamhub.api.v1.payment.PaymentService}, which the order-domain
 * {@code OrderReceiptDto} intentionally omits.
 *
 * @param id        receipt id
 * @param orderId   owning order id
 * @param kind      PAY / REFUND
 * @param amount    amount
 * @param method    pay method
 * @param memo      note (masked card for mock approvals)
 * @param provider  issuing PG code
 * @param txnId     transaction id
 * @param createdAt issued at
 */
public record PaymentReceiptDto(
        Long id,
        Long orderId,
        ReceiptKind kind,
        Long amount,
        String method,
        String memo,
        String provider,
        String txnId,
        LocalDateTime createdAt) {

    /** Maps an {@link OrderReceipt} entity to the payment receipt DTO. */
    public static PaymentReceiptDto from(OrderReceipt receipt) {
        return new PaymentReceiptDto(
                receipt.getId(),
                receipt.getOrderId(),
                receipt.getKind(),
                receipt.getAmount(),
                receipt.getMethod(),
                receipt.getMemo(),
                receipt.getProvider(),
                receipt.getTxnId(),
                receipt.getCreatedAt());
    }
}
