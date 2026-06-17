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

/**
 * A goods order. The table is named {@code ORDERS} because {@code ORDER} is a SQL
 * reserved word; the entity class keeps the natural name {@code Order}.
 */
@Entity
@Table(name = "ORDERS", indexes = {
        @Index(name = "idx_orders_member", columnList = "member_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_ordered_at", columnList = "ordered_at"),
        @Index(name = "idx_orders_order_no", columnList = "order_no")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Order number, e.g. {@code YYYYMMDD-XXXXXX}. */
    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    /** FK → MEMBER. */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private OrderStatus status;

    @Column(name = "ordered_name", nullable = false, length = 50)
    private String orderedName;

    @Column(name = "ordered_phone", length = 20)
    private String orderedPhone;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "receiver_addr", length = 300)
    private String receiverAddr;

    /** Goods subtotal (including option extra charges). */
    @Column(name = "goods_total", nullable = false)
    private Long goodsTotal;

    @Column(name = "ship_fee", nullable = false)
    private Long shipFee;

    @Column(name = "coupon_discount", nullable = false)
    private Long couponDiscount;

    @Column(name = "point_used", nullable = false)
    private Long pointUsed;

    /** Final total = goodsTotal + shipFee − couponDiscount − pointUsed (floored at 0). */
    @Column(name = "total", nullable = false)
    private Long total;

    /** {@code BANK} / {@code CARD}. */
    @Column(name = "pay_method", nullable = false, length = 20)
    private String payMethod;

    @Column(name = "tracking_no", length = 50)
    private String trackingNo;

    @Column(name = "ship_company", length = 50)
    private String shipCompany;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Order(String orderNo, Long memberId, OrderStatus status, String orderedName,
                  String orderedPhone, String receiverName, String receiverPhone,
                  String receiverAddr, Long goodsTotal, Long shipFee, Long couponDiscount,
                  Long pointUsed, Long total, String payMethod, String trackingNo,
                  String shipCompany, LocalDateTime orderedAt) {
        this.orderNo = orderNo;
        this.memberId = memberId;
        this.status = status;
        this.orderedName = orderedName;
        this.orderedPhone = orderedPhone;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddr = receiverAddr;
        this.goodsTotal = goodsTotal != null ? goodsTotal : 0L;
        this.shipFee = shipFee != null ? shipFee : 0L;
        this.couponDiscount = couponDiscount != null ? couponDiscount : 0L;
        this.pointUsed = pointUsed != null ? pointUsed : 0L;
        this.total = total;
        this.payMethod = payMethod;
        this.trackingNo = trackingNo;
        this.shipCompany = shipCompany;
        this.orderedAt = orderedAt != null ? orderedAt : LocalDateTime.now();
        this.updatedAt = this.orderedAt;
        if (this.total == null) {
            recalcTotal(this.goodsTotal);
        }
    }

    /**
     * Recomputes {@code total} from the given goods subtotal and the stored fees/discounts.
     * Single source of truth used by the seed generator and future order creation.
     */
    public void recalcTotal(long goodsTotal) {
        this.goodsTotal = goodsTotal;
        long computed = goodsTotal + this.shipFee - this.couponDiscount - this.pointUsed;
        this.total = Math.max(0L, computed);
        this.updatedAt = LocalDateTime.now();
    }

    /** Transitions the order status (transition legality is enforced by the service). */
    public void changeStatus(OrderStatus to) {
        this.status = to;
        this.updatedAt = LocalDateTime.now();
    }

    /** Sets shipment tracking info. */
    public void setTracking(String trackingNo, String shipCompany) {
        this.trackingNo = trackingNo;
        this.shipCompany = shipCompany;
        this.updatedAt = LocalDateTime.now();
    }
}
