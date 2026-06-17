package org.streamhub.api.v1.donation.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.donation.entity.Subscription;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;

/**
 * JPA repository for {@link Subscription}. The joined paginated listing lives in MyBatis;
 * this exposes the CRON billing scan and plan-usage counts.
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** Due-billing scan — uses {@code idx_subscription_next_billing}. */
    List<Subscription> findByStatusAndNextBillingAtBefore(SubscriptionStatus status, LocalDateTime now);

    long countByPlanIdAndStatus(Long planId, SubscriptionStatus status);
}
