package org.streamhub.api.v1.coupon;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.coupon.dto.CouponDto;
import org.streamhub.api.v1.coupon.dto.CouponSearchRequest;
import org.streamhub.api.v1.coupon.entity.Coupon;
import org.streamhub.api.v1.coupon.repository.CouponRepository;

/**
 * Discount-coupon management: admin CRUD plus a filtered listing. The demo dataset is small,
 * so listing loads all coupons and filters/sorts in memory (newest first) — no query
 * specialization needed.
 */
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final ActionLogPublisher actionLogPublisher;

    public CouponService(CouponRepository couponRepository, ActionLogPublisher actionLogPublisher) {
        this.couponRepository = couponRepository;
        this.actionLogPublisher = actionLogPublisher;
    }

    /** Admin listing: all coupons newest first, optionally filtered by useYn / keyword. */
    @Transactional(readOnly = true)
    public List<CouponDto> list(CouponSearchRequest request) {
        String useYn = request != null ? request.useYn() : null;
        String keyword = request != null && request.keyword() != null
                ? request.keyword().trim().toLowerCase() : null;
        return couponRepository.findAll().stream()
                .filter(coupon -> useYn == null || useYn.isBlank() || useYn.equals(coupon.getUseYn()))
                .filter(coupon -> keyword == null || keyword.isBlank()
                        || coupon.getCode().toLowerCase().contains(keyword)
                        || coupon.getName().toLowerCase().contains(keyword))
                .sorted(Comparator.comparing(Coupon::getId).reversed())
                .map(CouponDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponDto detail(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return CouponDto.from(coupon);
    }

    @Transactional
    public CouponDto create(CouponDto request) {
        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .roundUnit(request.getRoundUnit())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .useYn(defaultYn(request.getUseYn()))
                .build();
        Coupon saved = couponRepository.save(coupon);
        actionLogPublisher.publish("COUPON_CREATE", "COUPON", String.valueOf(saved.getId()), request.getName());
        return CouponDto.from(saved);
    }

    @Transactional
    public CouponDto update(Long id, CouponDto request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        coupon.update(
                request.getCode(), request.getName(), request.getDiscountType(),
                request.getDiscountValue(), request.getMinOrderAmount(), request.getMaxDiscountAmount(),
                request.getRoundUnit(), request.getStartAt(), request.getEndAt(),
                defaultYn(request.getUseYn()));
        couponRepository.saveAndFlush(coupon);
        actionLogPublisher.publish("COUPON_UPDATE", "COUPON", String.valueOf(id), request.getName());
        return CouponDto.from(coupon);
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        couponRepository.delete(coupon);
        actionLogPublisher.publish("COUPON_DELETE", "COUPON", String.valueOf(id), coupon.getName());
    }

    // --- helpers -----------------------------------------------------------

    private String defaultYn(String value) {
        return value == null || value.isBlank() ? "Y" : value;
    }
}
