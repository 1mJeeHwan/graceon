package org.streamhub.api.v1.order.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.order.entity.OrderStatus;

/** One row of the order list, joined with the member name and line-item count. */
@Getter
@Setter
@NoArgsConstructor
public class OrderListItem {
    private Long id;
    private String orderNo;
    private Long memberId;
    private String memberName;
    private OrderStatus status;
    private String orderedName;
    private String receiverName;
    private Long total;
    private String payMethod;
    private String trackingNo;
    private Integer itemCount;
    private LocalDateTime orderedAt;
}
