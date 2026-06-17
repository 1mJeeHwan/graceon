package org.streamhub.api.v1.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A payment/refund receipt record attached to an {@link Order}. */
@Entity
@Table(name = "ORDER_RECEIPT", indexes = {
        @Index(name = "idx_order_receipt_order", columnList = "order_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → ORDERS. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 8)
    private ReceiptKind kind;

    @Column(name = "amount", nullable = false)
    private Long amount;

    /** {@code BANK} / {@code CARD}. */
    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private OrderReceipt(Long orderId, ReceiptKind kind, Long amount, String method,
                         String memo, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.kind = kind;
        this.amount = amount;
        this.method = method;
        this.memo = memo;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
