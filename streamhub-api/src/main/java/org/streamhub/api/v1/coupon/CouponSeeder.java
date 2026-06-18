package org.streamhub.api.v1.coupon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.coupon.entity.Coupon;
import org.streamhub.api.v1.coupon.entity.DiscountType;
import org.streamhub.api.v1.coupon.repository.CouponRepository;

/**
 * Seeds the discount-coupon demo dataset. Idempotent (skips when the coupon table already
 * holds rows). The fixed-seed {@link Random} makes the dataset <em>shape</em> reproducible
 * across runs; the absolute dates are <em>not</em> fixed — every row is anchored to
 * {@link LocalDateTime#now()}, so the validity windows roll forward to stay current (some
 * already expired). All values are demo/fictional (no real promotion data — PII guard).
 */
@Slf4j
@Component
@Order(12)
public class CouponSeeder implements CommandLineRunner {

    private static final long SEED = 912L;
    private static final int TARGET_COUPONS = 30;

    /** Fixed AMOUNT coupons (code, name, 원 할인, 최소주문액, 절사단위). */
    private static final Object[][] AMOUNT_COUPONS = {
            {"WELCOME5000", "신규가입 5천원 할인", 5000, 10000, 0},
            {"WELCOME3000", "신규가입 3천원 할인", 3000, 0, 0},
            {"FIRSTBUY10000", "첫 구매 1만원 할인", 10000, 50000, 100},
            {"THANKS3000", "감사 3천원 쿠폰", 3000, 20000, 0},
            {"REVIEW5000", "리뷰 작성 5천원 적립", 5000, 30000, 100},
            {"BIRTHDAY10000", "생일 축하 1만원 쿠폰", 10000, 40000, 0},
            {"REPURCHASE5000", "재구매 5천원 할인", 5000, 25000, 10},
            {"OFFLINE3000", "오프라인 매장 3천원", 3000, 15000, 0},
            {"BOOK5000", "신앙도서 5천원 할인", 5000, 20000, 100},
            {"NEWYEAR10000", "새해맞이 1만원 쿠폰", 10000, 60000, 0},
            {"EVENT3000", "이벤트 참여 3천원", 3000, 10000, 10},
            {"FRIEND5000", "친구초대 5천원 적립", 5000, 30000, 0},
    };

    /** Fixed PERCENT coupons (code, name, 퍼센트, 최소주문액, 할인상한, 절사단위). */
    private static final Object[][] PERCENT_COUPONS = {
            {"PRAISE10", "찬양앨범 10% 쿠폰", 10, 10000, 5000, 100},
            {"XMAS10", "성탄절 특별 쿠폰", 10, 20000, 10000, 100},
            {"EASTER15", "부활절 15% 할인", 15, 30000, 15000, 100},
            {"SUMMER5", "여름맞이 5% 쿠폰", 5, 0, 3000, 10},
            {"GRADE20", "VIP 등급 20% 할인", 20, 50000, 20000, 100},
            {"MUSIC15", "음원 15% 할인", 15, 15000, 8000, 10},
            {"WORSHIP10", "예배용품 10% 쿠폰", 10, 25000, 7000, 100},
            {"AUTUMN10", "가을 시즌 10% 할인", 10, 20000, 10000, 100},
            {"MEMBER5", "회원 전용 5% 쿠폰", 5, 10000, 5000, 0},
            {"BIBLE15", "성경 15% 할인", 15, 30000, 12000, 100},
            {"WEEKEND10", "주말 특가 10% 쿠폰", 10, 15000, 6000, 10},
            {"GIFT20", "선물세트 20% 할인", 20, 40000, 25000, 100},
            {"CHURCH10", "교회단체 10% 쿠폰", 10, 100000, 30000, 100},
            {"SPRING15", "봄맞이 15% 할인", 15, 20000, 9000, 100},
            {"FLASH20", "타임세일 20% 쿠폰", 20, 30000, 15000, 100},
            {"DEVOTION5", "묵상집 5% 할인", 5, 12000, 4000, 10},
            {"KIDS10", "어린이 도서 10% 쿠폰", 10, 18000, 7000, 100},
            {"YEAREND15", "연말결산 15% 할인", 15, 35000, 14000, 100},
    };

    private final CouponRepository couponRepository;

    public CouponSeeder(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public void run(String... args) {
        if (couponRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);

        List<Coupon> coupons = new ArrayList<>();
        for (Object[] row : AMOUNT_COUPONS) {
            coupons.add(buildAmount(row, now, rnd));
        }
        for (Object[] row : PERCENT_COUPONS) {
            coupons.add(buildPercent(row, now, rnd));
        }

        couponRepository.saveAll(coupons);
        log.info("Seeded {} coupons", coupons.size());
    }

    private Coupon buildAmount(Object[] row, LocalDateTime now, Random rnd) {
        LocalDateTime startAt = now.minusDays(1 + rnd.nextInt(40));
        LocalDateTime endAt = now.plusDays(10 + rnd.nextInt(81));
        if (rnd.nextInt(100) < 20) { // ~20% already expired
            endAt = now.minusDays(1 + rnd.nextInt(5));
        }
        return Coupon.builder()
                .code((String) row[0])
                .name((String) row[1])
                .discountType(DiscountType.AMOUNT)
                .discountValue((int) row[2])
                .minOrderAmount((int) row[3])
                .maxDiscountAmount(null)
                .roundUnit((int) row[4])
                .startAt(startAt)
                .endAt(endAt)
                .useYn(rnd.nextInt(100) < 80 ? "Y" : "N")
                .createdAt(startAt)
                .build();
    }

    private Coupon buildPercent(Object[] row, LocalDateTime now, Random rnd) {
        LocalDateTime startAt = now.minusDays(1 + rnd.nextInt(40));
        LocalDateTime endAt = now.plusDays(10 + rnd.nextInt(81));
        if (rnd.nextInt(100) < 20) { // ~20% already expired
            endAt = now.minusDays(1 + rnd.nextInt(5));
        }
        return Coupon.builder()
                .code((String) row[0])
                .name((String) row[1])
                .discountType(DiscountType.PERCENT)
                .discountValue((int) row[2])
                .minOrderAmount((int) row[3])
                .maxDiscountAmount((Integer) row[4])
                .roundUnit((int) row[5])
                .startAt(startAt)
                .endAt(endAt)
                .useYn(rnd.nextInt(100) < 80 ? "Y" : "N")
                .createdAt(startAt)
                .build();
    }
}
