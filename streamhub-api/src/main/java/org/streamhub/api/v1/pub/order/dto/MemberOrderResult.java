package org.streamhub.api.v1.pub.order.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.order.entity.OrderStatus;

/**
 * Result of a public album purchase: the created order, transitioned to {@code PAID} via the
 * mock payment flow. {@code testMode} is always true (실 PG 미연동 — 가짜 승인).
 *
 * @param orderNo  business order number ({@code YYYYMMDD-XXXXXX})
 * @param status   resulting order status (PAID on success)
 * @param total    server-computed charged amount
 * @param paidAt   timestamp of the PAY receipt
 * @param testMode always true in the demo
 */
public record MemberOrderResult(
        String orderNo,
        OrderStatus status,
        Long total,
        LocalDateTime paidAt,
        boolean testMode) {
}
