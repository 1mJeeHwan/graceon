package org.streamhub.api.v1.coupon.dto;

/**
 * Coupon listing filter. Both fields are optional: {@code useYn} narrows by usage flag
 * ("Y"/"N") and {@code keyword} matches the code or name (case-insensitive, contains).
 *
 * @param useYn   optional usage flag filter ("Y"/"N")
 * @param keyword optional code/name substring filter
 */
public record CouponSearchRequest(String useYn, String keyword) {
}
