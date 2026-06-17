package org.streamhub.api.v1.donation.entity;

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

/** A single donation record — one billing cycle or one one-off gift. */
@Entity
@Table(name = "DONATION", indexes = {
        @Index(name = "idx_donation_member", columnList = "member_id"),
        @Index(name = "idx_donation_subscription", columnList = "subscription_id"),
        @Index(name = "idx_donation_status_paid", columnList = "status, paid_at"),
        @Index(name = "idx_donation_type", columnList = "type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → MEMBER. */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** FK → SUBSCRIPTION (null for one-off donations). */
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DonationType type;

    /** Charged amount (KRW). */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** Subscription cycle number (null for one-off donations). */
    @Column(name = "cycle_no")
    private Integer cycleNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DonationStatus status;

    /** Points awarded for this donation (default 0). */
    @Column(name = "point_awarded", nullable = false)
    private Long pointAwarded;

    /** Demo-safety label — always "Y" (no real PG integration). */
    @Column(name = "test_mode", nullable = false, length = 1)
    private String testMode;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Donation(Long memberId, Long subscriptionId, DonationType type, Long amount,
                     Integer cycleNo, DonationStatus status, Long pointAwarded,
                     String testMode, LocalDateTime paidAt, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.subscriptionId = subscriptionId;
        this.type = type;
        this.amount = amount;
        this.cycleNo = cycleNo;
        this.status = status;
        this.pointAwarded = pointAwarded != null ? pointAwarded : 0L;
        this.testMode = testMode != null ? testMode : "Y";
        this.paidAt = paidAt != null ? paidAt : LocalDateTime.now();
        this.createdAt = createdAt != null ? createdAt : this.paidAt;
    }
}
