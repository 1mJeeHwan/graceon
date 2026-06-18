package org.streamhub.api.v1.pub.order.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.order.entity.OrderStatus;

/**
 * One row of a member's purchase history ("구매내역").
 *
 * @param orderNo     business order number
 * @param productName first line-item name (album title)
 * @param total       order total
 * @param status      order status
 * @param orderedAt   order time
 */
public record MemberOrderListItem(
        String orderNo,
        String productName,
        Long total,
        OrderStatus status,
        LocalDateTime orderedAt) {
}
