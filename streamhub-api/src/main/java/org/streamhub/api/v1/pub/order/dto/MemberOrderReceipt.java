package org.streamhub.api.v1.pub.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.streamhub.api.v1.order.entity.OrderStatus;

/**
 * Receipt detail for one of the member's own orders ("영수증 상세"). Aggregates the order header,
 * its line items, and the payment receipt into the single payload the mypage receipt screen renders.
 *
 * @param orderNo        business order number
 * @param status         order status
 * @param orderedName    orderer name
 * @param orderedAt      order time
 * @param paidAt         payment-completed time (PAY receipt's createdAt; {@code null} if not paid)
 * @param items          ordered line items
 * @param goodsTotal     goods subtotal
 * @param shipFee        shipping fee
 * @param couponDiscount coupon discount applied
 * @param pointUsed      points spent
 * @param total          final charged total
 * @param payMethod      payment method ({@code null} if unknown)
 * @param payProvider    PG provider ({@code null} if unknown)
 * @param payStatus      payment status name ({@code null} if unknown)
 * @param txnId          payment transaction id ({@code null} if none)
 * @param receiverName   shipping receiver name ({@code null} if none)
 * @param receiverPhone  shipping receiver phone ({@code null} if none)
 * @param receiverAddr   shipping receiver address ({@code null} if none)
 * @param trackingNo     shipment tracking number ({@code null} if none)
 * @param shipCompany    courier company ({@code null} if none)
 */
public record MemberOrderReceipt(
        String orderNo,
        OrderStatus status,
        String orderedName,
        LocalDateTime orderedAt,
        LocalDateTime paidAt,
        List<Line> items,
        long goodsTotal,
        long shipFee,
        long couponDiscount,
        long pointUsed,
        long total,
        String payMethod,
        String payProvider,
        String payStatus,
        String txnId,
        String receiverName,
        String receiverPhone,
        String receiverAddr,
        String trackingNo,
        String shipCompany) {

    /**
     * One ordered line on the receipt.
     *
     * @param goodsName  goods name snapshot
     * @param optionName option name snapshot ({@code null} if none)
     * @param unitPrice  unit price at order time
     * @param qty        quantity ordered
     * @param lineTotal  {@code unitPrice × qty}
     */
    public record Line(
            String goodsName,
            String optionName,
            long unitPrice,
            int qty,
            long lineTotal) {
    }
}
