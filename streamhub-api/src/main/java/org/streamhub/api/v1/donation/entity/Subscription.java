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

/**
 * A recurring-donation subscription. The entity owns its own state-machine transitions;
 * illegal transitions throw {@link IllegalStateException} for the service to convert.
 */
@Entity
@Table(name = "SUBSCRIPTION", indexes = {
        @Index(name = "idx_subscription_member", columnList = "member_id"),
        @Index(name = "idx_subscription_status", columnList = "status"),
        @Index(name = "idx_subscription_next_billing", columnList = "status, next_billing_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → MEMBER. */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** FK → SUBSCRIPTION_PLAN. */
    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /** Masked demo billing key, e.g. {@code bk_****1234}. */
    @Column(name = "billing_key_masked", nullable = false, length = 40)
    private String billingKeyMasked;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    /** Cycles billed so far (starts at 0). */
    @Column(name = "cycle_no", nullable = false)
    private Integer cycleNo;

    /** Next scheduled billing time; {@code null} when PAUSED or CANCELED. */
    @Column(name = "next_billing_at")
    private LocalDateTime nextBillingAt;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Subscription(Long memberId, Long planId, String billingKeyMasked,
                         SubscriptionStatus status, Integer cycleNo, LocalDateTime nextBillingAt,
                         LocalDateTime startedAt, LocalDateTime canceledAt,
                         LocalDateTime createdAt) {
        this.memberId = memberId;
        this.planId = planId;
        this.billingKeyMasked = billingKeyMasked;
        this.status = status;
        this.cycleNo = cycleNo != null ? cycleNo : 0;
        this.nextBillingAt = nextBillingAt;
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.canceledAt = canceledAt;
        this.createdAt = createdAt != null ? createdAt : this.startedAt;
        this.updatedAt = this.createdAt;
    }

    /** Pauses an ACTIVE subscription and clears the next billing date. */
    public void pause() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("only an active subscription can be paused");
        }
        this.status = SubscriptionStatus.PAUSED;
        this.nextBillingAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** Resumes a PAUSED subscription, scheduling the next billing date. */
    public void resume(LocalDateTime nextBillingAt) {
        if (this.status != SubscriptionStatus.PAUSED) {
            throw new IllegalStateException("only a paused subscription can be resumed");
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.nextBillingAt = nextBillingAt;
        this.updatedAt = LocalDateTime.now();
    }

    /** Cancels the subscription (terminal). */
    public void cancel() {
        if (this.status == SubscriptionStatus.CANCELED) {
            throw new IllegalStateException("subscription is already canceled");
        }
        this.status = SubscriptionStatus.CANCELED;
        this.nextBillingAt = null;
        this.canceledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** Advances one billing cycle and schedules the next billing date (CRON only). */
    public void advanceCycle(LocalDateTime next) {
        this.cycleNo += 1;
        this.nextBillingAt = next;
        this.updatedAt = LocalDateTime.now();
    }
}
