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

    /** Issuing PG (C4 payment seam). e.g. {@code MOCK}. */
    @Column(name = "provider", length = 20)
    private String provider;

    /** Transaction id. Mock = {@code MOCK-{orderNo}-{seq}}; real key = PG paymentKey. */
    @Column(name = "txn_id", length = 60)
    private String txnId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private OrderReceipt(Long orderId, ReceiptKind kind, Long amount, String method,
                         String memo, String provider, String txnId, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.kind = kind;
        this.amount = amount;
        this.method = method;
        this.memo = memo;
        this.provider = provider;
        this.txnId = txnId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** Backfills the issuing provider and transaction id (C4 payment seam). */
    public void setProviderTxn(String provider, String txnId) {
        this.provider = provider;
        this.txnId = txnId;
    }
}
