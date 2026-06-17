package org.streamhub.api.v1.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The grace-point ledger — the single, append-only source of truth for every point
 * change. Manual grants/deductions, scheduled expiry recovery, and recurring-donation
 * accrual all write here, and {@link Member#getPointBalance()} caches the running sum.
 */
@Entity
@Table(name = "POINT_LEDGER", indexes = {
        @Index(name = "idx_point_ledger_member", columnList = "member_id"),
        @Index(name = "idx_point_ledger_created", columnList = "created_at"),
        @Index(name = "idx_point_ledger_expire", columnList = "status, expire_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → MEMBER (logical; held as an id, no JPA association). */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** Signed change; positive accrues, negative deducts/uses. */
    @Column(name = "delta", nullable = false)
    private long delta;

    /** Member balance after this entry was applied (audit integrity). */
    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private LedgerSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LedgerStatus status;

    /** Expiry time; {@code null} means it never expires. */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /** FK → DONATION (nullable; set when the entry originates from a donation). */
    @Column(name = "donation_id")
    private Long donationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PointLedger(Long memberId, long delta, long balanceAfter, String reason,
                        LedgerSourceType sourceType, LedgerStatus status,
                        LocalDateTime expireAt, Long donationId, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.sourceType = sourceType != null ? sourceType : LedgerSourceType.MANUAL;
        this.status = status != null ? status : LedgerStatus.ACTIVE;
        this.expireAt = expireAt;
        this.donationId = donationId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** Marks this accrual entry as expired (recovery is recorded as a separate row). */
    public void markExpired() {
        this.status = LedgerStatus.EXPIRED;
    }
}
