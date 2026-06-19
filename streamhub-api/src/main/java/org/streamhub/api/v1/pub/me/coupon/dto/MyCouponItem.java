package org.streamhub.api.v1.pub.me.coupon.dto;

import java.time.LocalDate;

/**
 * One coupon in the member's coupon box ("쿠폰함"). Coupons are a global code model (not per-member
 * issuance), so the list is the set of currently usable global coupons with the member's own
 * {@code used} state layered on. Maps from {@link org.streamhub.api.v1.coupon.entity.Coupon}:
 * {@code validFrom}/{@code validUntil} are the {@code startAt}/{@code endAt} dates and
 * {@code discountType} is the enum name.
 *
 * @param id            coupon id
 * @param code          coupon code
 * @param name          coupon name
 * @param discountType  {@code AMOUNT} or {@code PERCENT}
 * @param discountValue 할인 값 (원 for {@code AMOUNT}, percent for {@code PERCENT})
 * @param minOrderAmount minimum order amount to apply
 * @param validFrom     valid-from date
 * @param validUntil    valid-until date
 * @param used          whether this member has already redeemed this coupon
 */
public record MyCouponItem(
        Long id,
        String code,
        String name,
        String discountType,
        long discountValue,
        long minOrderAmount,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean used) {
}
