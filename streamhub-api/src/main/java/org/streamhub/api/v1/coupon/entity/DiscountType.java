package org.streamhub.api.v1.coupon.entity;

/**
 * Coupon discount calculation mode. Stored via {@code @Enumerated(STRING)}.
 */
public enum DiscountType {
    /** 정액 할인 (원 단위). */
    AMOUNT,
    /** 정률 할인 (퍼센트, {@code maxDiscountAmount} 상한 적용 가능). */
    PERCENT
}
