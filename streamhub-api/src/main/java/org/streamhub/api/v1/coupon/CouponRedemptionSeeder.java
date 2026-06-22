package org.streamhub.api.v1.coupon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.coupon.entity.Coupon;
import org.streamhub.api.v1.coupon.entity.CouponRedemption;
import org.streamhub.api.v1.coupon.repository.CouponRedemptionRepository;
import org.streamhub.api.v1.coupon.repository.CouponRepository;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.repository.MemberRepository;

/**
 * Backfills the coupon redemption ledger ({@code COUPON_REDEMPTION}) with demo usage history so the
 * admin "쿠폰 사용 내역" drill-down shows real rows. Runs after members ({@link Order} 1) and coupons
 * ({@code CouponSeeder} @Order 12) exist.
 *
 * <p><b>Idempotent / non-destructive:</b> seeds only when the ledger is empty — i.e. no real
 * redemption has happened through the redeem flow (which is the unique-constraint enforcement point).
 * For each seeded coupon it links distinct members and sets {@code usedCount} to the number of rows
 * created, so the coupon list's "사용 N/M" matches the modal. Unlike {@code CouponSeeder}, the guard
 * is the ledger count (not the coupon count), so it also populates an already-seeded live DB on the
 * next boot.
 */
@Slf4j
@Component
@Order(22)
public class CouponRedemptionSeeder implements CommandLineRunner {

    private static final long SEED = 922L;
    /** How many coupons get demo usage history (the rest stay at zero redemptions). */
    private static final int MAX_COUPONS = 12;
    /** Upper bound on redemptions per seeded coupon (also capped by member count / usage limit). */
    private static final int MAX_PER_COUPON = 5;

    private final CouponRedemptionRepository redemptionRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;

    public CouponRedemptionSeeder(CouponRedemptionRepository redemptionRepository,
                                  CouponRepository couponRepository,
                                  MemberRepository memberRepository) {
        this.redemptionRepository = redemptionRepository;
        this.couponRepository = couponRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) {
        if (redemptionRepository.count() > 0) {
            return; // real redemption ledger exists — never overwrite
        }
        List<Long> memberIds = memberRepository.findAll().stream()
                .map(Member::getId)
                .toList();
        List<Coupon> coupons = couponRepository.findAll();
        if (memberIds.isEmpty() || coupons.isEmpty()) {
            return;
        }

        Random rnd = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();
        List<CouponRedemption> rows = new ArrayList<>();
        int seededCoupons = 0;

        for (Coupon coupon : coupons) {
            if (seededCoupons >= MAX_COUPONS) {
                break;
            }
            int cap = Math.min(MAX_PER_COUPON, memberIds.size());
            if (coupon.getUsageLimit() != null) {
                cap = Math.min(cap, coupon.getUsageLimit());
            }
            if (cap <= 0) {
                continue;
            }
            int count = 1 + rnd.nextInt(cap);

            List<Long> shuffled = new ArrayList<>(memberIds);
            Collections.shuffle(shuffled, rnd);
            for (Long memberId : shuffled.subList(0, count)) {
                rows.add(CouponRedemption.builder()
                        .couponId(coupon.getId())
                        .memberId(memberId)
                        .redeemedAt(now.minusDays(rnd.nextInt(60)).minusHours(rnd.nextInt(24)))
                        .build());
            }
            couponRepository.setUsedCount(coupon.getId(), count);
            seededCoupons++;
        }

        redemptionRepository.saveAll(rows);
        log.info("Seeded {} coupon redemptions across {} coupons", rows.size(), seededCoupons);
    }
}
