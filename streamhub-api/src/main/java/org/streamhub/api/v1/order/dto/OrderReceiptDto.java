package org.streamhub.api.v1.order.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.order.entity.OrderReceipt;
import org.streamhub.api.v1.order.entity.ReceiptKind;

/** A payment/refund receipt record for the order detail view. */
public record OrderReceiptDto(
        Long id,
        ReceiptKind kind,
        Long amount,
        String method,
        String memo,
        LocalDateTime createdAt) {

    /** Maps an {@link OrderReceipt} entity to its DTO. */
    public static OrderReceiptDto from(OrderReceipt receipt) {
        return new OrderReceiptDto(
                receipt.getId(),
                receipt.getKind(),
                receipt.getAmount(),
                receipt.getMethod(),
                receipt.getMemo(),
                receipt.getCreatedAt());
    }
}
