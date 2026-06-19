package org.streamhub.api.v1.coupon.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.coupon.entity.Coupon;

/** JPA repository for {@link Coupon} (discount coupons). */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCode(String code);

    /** Looks up a coupon by its unique code (redemption path). */
    Optional<Coupon> findByCode(String code);

    /**
     * Currently usable global coupons at {@code now}: enabled and within their valid window, newest
     * first. Backs the member coupon-box ("쿠폰함"); per-member used/exhausted state is layered on in
     * the service.
     */
    @Query("SELECT c FROM Coupon c WHERE c.useYn = 'Y' AND c.startAt <= :now AND c.endAt >= :now "
            + "ORDER BY c.id DESC")
    List<Coupon> findActiveAt(@Param("now") LocalDateTime now);

    /**
     * Atomically consumes one global use: increments {@code usedCount} only while the coupon is not
     * yet exhausted, in a single conditional UPDATE. Returns the number of rows updated — {@code 1}
     * on success, {@code 0} when the global limit is already reached. This closes the lost-update
     * race of a read-modify-write {@code usedCount++}: concurrent redeemers cannot both observe the
     * same pre-increment count. An unlimited coupon ({@code usageLimit IS NULL}) always increments.
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 "
            + "WHERE c.id = :id AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    int incrementUsedCount(@Param("id") Long id);

    /**
     * Atomically releases one consumed use: decrements {@code usedCount} only while it is positive, in
     * a single conditional UPDATE. Returns the number of rows updated — {@code 1} when a use was
     * released, {@code 0} when {@code usedCount} was already {@code 0}. The {@code usedCount > 0} guard
     * makes the release idempotent (a double-release cannot drive the counter negative) and mirrors the
     * lost-update-safe {@link #incrementUsedCount} on the consumption side.
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount - 1 "
            + "WHERE c.id = :id AND c.usedCount > 0")
    int decrementUsedCount(@Param("id") Long id);
}
