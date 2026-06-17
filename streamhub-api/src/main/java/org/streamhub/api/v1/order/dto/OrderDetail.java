package org.streamhub.api.v1.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.order.entity.OrderStatus;

/**
 * Full order detail. Base fields come from MyBatis; line items and receipts are
 * loaded and filled by the service.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderDetail {
    private Long id;
    private String orderNo;
    private Long memberId;
    private String memberName;
    private OrderStatus status;
    private String orderedName;
    private String orderedPhone;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddr;
    private Long goodsTotal;
    private Long shipFee;
    private Long couponDiscount;
    private Long pointUsed;
    private Long total;
    private String payMethod;
    private String trackingNo;
    private String shipCompany;
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDto> items;       // filled by the service
    private List<OrderReceiptDto> receipts; // filled by the service
}
