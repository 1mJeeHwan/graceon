package org.streamhub.api.v1.payment.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.order.entity.PayStatus;
import org.streamhub.api.v1.order.entity.ReceiptKind;

/**
 * One row of the payment-history list: an {@code ORDER_RECEIPT} record (a payment or refund)
 * joined with its order and the paying member. This is the read model behind the admin
 * 결제내역 screen — distinct from the order list, which is keyed on the order itself.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentListItem {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long memberId;
    private String memberName;
    private ReceiptKind kind;
    private Long amount;
    private String method;
    private String provider;
    private String txnId;
    private String memo;
    private PayStatus payStatus;
    private LocalDateTime createdAt;
}
