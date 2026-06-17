package org.streamhub.api.v1.donation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;

/** JPA repository for {@link SubscriptionPlan}. */
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findAllByOrderByPriceAscIdAsc();
}
