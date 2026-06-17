package org.streamhub.api.v1.sms.entity;

/** SMS send trigger (C6). Stored via {@code @Enumerated(STRING)}. */
public enum SmsKind {
    /** 직접 발송. */
    CUSTOM,
    /** 주문 결제완료 알림. */
    ORDER_PAID,
    /** 주문 배송 알림. */
    ORDER_SHIPPING,
    /** 단건 후원 영수. */
    DONATION_ONCE
}
