package org.streamhub.api.v1.pub.me.coupon;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.coupon.entity.Coupon;
import org.streamhub.api.v1.coupon.repository.CouponRedemptionRepository;
import org.streamhub.api.v1.coupon.repository.CouponRepository;
import org.streamhub.api.v1.pub.me.coupon.dto.MyCouponItem;

/**
 * Reads the authenticated member's coupon box ("쿠폰함"). Coupons are a global code model (not
 * per-member issuance), so this lists the currently usable global coupons and computes {@code used}
 * per member from {@code COUPON_REDEMPTION}. The member id is resolved by the controller from the
 * Bearer member token, so this service is already scoped to one member.
 */
@Service
public class MemberCouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    public MemberCouponService(CouponRepository couponRepository,
                               CouponRedemptionRepository couponRedemptionRepository) {
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
    }

    /**
     * Returns the currently usable (enabled, within-window) global coupons with this member's
     * {@code used} flag layered on.
     *
     * @param memberId authenticated member id
     * @return the member's coupon-box items
     */
    @Transactional(readOnly = true)
    public List<MyCouponItem> coupons(Long memberId) {
        List<Coupon> active = couponRepository.findActiveAt(LocalDateTime.now());
        Set<Long> redeemedCouponIds = Set.copyOf(couponRedemptionRepository.findCouponIdsByMemberId(memberId));
        return active.stream()
                .map(coupon -> toItem(coupon, redeemedCouponIds.contains(coupon.getId())))
                .toList();
    }

    private MyCouponItem toItem(Coupon coupon, boolean used) {
        return new MyCouponItem(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null,
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getStartAt() != null ? coupon.getStartAt().toLocalDate() : null,
                coupon.getEndAt() != null ? coupon.getEndAt().toLocalDate() : null,
                used);
    }
}
