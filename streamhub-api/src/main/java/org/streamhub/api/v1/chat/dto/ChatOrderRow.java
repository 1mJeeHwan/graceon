package org.streamhub.api.v1.chat.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.order.entity.OrderStatus;

/** A single order row returned to the chatbot's order-lookup tool (C5). */
@Getter
@Setter
@NoArgsConstructor
public class ChatOrderRow {
    private String orderNo;
    private OrderStatus status;
    private String orderedName;
    private Long total;
    private String trackingNo;
    private String shipCompany;
    private LocalDateTime orderedAt;
}
