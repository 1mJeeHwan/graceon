package org.streamhub.api.v1.donation;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.donation.entity.Donation;
import org.streamhub.api.v1.donation.entity.DonationStatus;
import org.streamhub.api.v1.donation.entity.DonationType;
import org.streamhub.api.v1.donation.entity.Subscription;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;
import org.streamhub.api.v1.donation.repository.DonationRepository;
import org.streamhub.api.v1.donation.repository.SubscriptionPlanRepository;
import org.streamhub.api.v1.donation.repository.SubscriptionRepository;

/**
 * Per-cycle billing transaction boundary called by {@link BillingScheduler}. Each cycle charge —
 * donation record, point accrual, and subscription advance — commits atomically so a single
 * failed subscription cannot poison the whole batch. Test mode only; no real PG is invoked.
 */
@Service
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final DonationRepository donationRepository;
    private final PointLedgerWriter pointLedgerWriter;
    private final ActionLogPublisher actionLogPublisher;

    public BillingService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            DonationRepository donationRepository,
            PointLedgerWriter pointLedgerWriter,
            ActionLogPublisher actionLogPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.donationRepository = donationRepository;
        this.pointLedgerWriter = pointLedgerWriter;
        this.actionLogPublisher = actionLogPublisher;
    }

    /**
     * Charges a single billing cycle for the given subscription and advances its next billing date.
     * Re-reads the subscription inside the transaction and bails out if it is no longer ACTIVE
     * (defends against a pause/cancel that landed between the scan and this charge).
     */
    @Transactional
    public void chargeOneCycle(Long subscriptionId, LocalDateTime now) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return;
        }
        SubscriptionPlan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));

        long point = plan.getPrice() * plan.getPointRate() / 100;
        int cycleNo = subscription.getCycleNo() + 1;

        Donation donation = donationRepository.save(Donation.builder()
                .memberId(subscription.getMemberId())
                .subscriptionId(subscription.getId())
                .type(DonationType.SUBSCRIPTION)
                .amount(plan.getPrice())
                .cycleNo(cycleNo)
                .status(DonationStatus.PAID)
                .pointAwarded(point)
                .testMode("Y")
                .paidAt(now)
                .build());

        pointLedgerWriter.append(subscription.getMemberId(), point,
                "정기후원 " + cycleNo + "회차 적립", donation.getId());

        subscription.advanceCycle(now.plusMonths(plan.getPeriodMonths()));
        subscriptionRepository.saveAndFlush(subscription);

        actionLogPublisher.publish("BILLING_CHARGE", "SUBSCRIPTION",
                String.valueOf(subscription.getId()), "회차 " + cycleNo + " / ₩" + plan.getPrice());
    }
}
