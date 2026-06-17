package org.streamhub.api.v1.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A line item (cart snapshot) of an {@link Order}. */
@Entity
@Table(name = "ORDER_ITEM", indexes = {
        @Index(name = "idx_order_item_order", columnList = "order_id"),
        @Index(name = "idx_order_item_goods", columnList = "goods_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → ORDERS. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** FK → GOODS_ITEM (snapshot — retained even if the goods is deleted). */
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    /** FK → GOODS_OPTION (nullable). */
    @Column(name = "option_id")
    private Long optionId;

    /** Goods name snapshot at order time. */
    @Column(name = "goods_name", nullable = false, length = 200)
    private String goodsName;

    /** Option name snapshot at order time. */
    @Column(name = "option_name", length = 100)
    private String optionName;

    /** Unit price (option extra charge included), at order time. */
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    /** {@code unitPrice × qty}. */
    @Column(name = "line_total", nullable = false)
    private Long lineTotal;

    @Builder
    private OrderItem(Long orderId, Long goodsId, Long optionId, String goodsName,
                      String optionName, Long unitPrice, Integer qty, Long lineTotal) {
        this.orderId = orderId;
        this.goodsId = goodsId;
        this.optionId = optionId;
        this.goodsName = goodsName;
        this.optionName = optionName;
        this.unitPrice = unitPrice;
        this.qty = qty;
        this.lineTotal = lineTotal != null ? lineTotal
                : (unitPrice != null && qty != null ? unitPrice * qty : 0L);
    }
}
