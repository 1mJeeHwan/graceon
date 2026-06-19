package org.streamhub.api.v1.pub.me.donation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.donation.entity.Subscription;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;
import org.streamhub.api.v1.donation.repository.SubscriptionPlanRepository;
import org.streamhub.api.v1.pub.me.donation.dto.MyDonationItem;

/**
 * Read service for a logged-in member's recurring-donation subscriptions under the public
 * namespace. Every subscription is scoped to the resolved member id and returned most-recent first.
 * Plan name, amount, and billing cycle are joined from {@link SubscriptionPlan}.
 */
@Slf4j
@Service
public class MemberDonationService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    public MemberDonationService(MemberSubscriptionRepository subscriptionRepository,
                                 SubscriptionPlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    /** Returns the member's subscriptions (newest first), joined with their plan details. */
    @Transactional(readOnly = true)
    public List<MyDonationItem> donations(Long memberId) {
        List<Subscription> subscriptions = subscriptionRepository.findByMemberIdOrderByStartedAtDesc(memberId);
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = subscriptions.stream().map(Subscription::getPlanId).distinct().toList();
        Map<Long, SubscriptionPlan> plansById = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, Function.identity()));
        return subscriptions.stream()
                .map(subscription -> toItem(subscription, plansById.get(subscription.getPlanId())))
                .toList();
    }

    private MyDonationItem toItem(Subscription subscription, SubscriptionPlan plan) {
        String name = plan != null ? plan.getName() : null;
        long amount = plan != null && plan.getPrice() != null ? plan.getPrice() : 0L;
        String cycle = plan != null ? cycleOf(plan.getPeriodMonths()) : null;
        return new MyDonationItem(
                subscription.getId(),
                name,
                amount,
                cycle,
                subscription.getStatus() != null ? subscription.getStatus().name() : null,
                subscription.getNextBillingAt(),
                subscription.getStartedAt());
    }

    /** Derives a human-readable billing cycle label from the plan's period in months. */
    private String cycleOf(Integer periodMonths) {
        if (periodMonths == null || periodMonths <= 1) {
            return "MONTHLY";
        }
        return "EVERY_" + periodMonths + "_MONTHS";
    }
}
