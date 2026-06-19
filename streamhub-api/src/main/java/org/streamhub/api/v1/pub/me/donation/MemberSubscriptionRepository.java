package org.streamhub.api.v1.pub.me.donation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.donation.entity.Subscription;

/**
 * Read-side repository for a member's own recurring-donation subscriptions. Kept separate from the
 * admin {@code SubscriptionRepository} so the public {@code /pub/v1/me} feature owns its own query
 * surface. Scoped by {@code memberId} (uses {@code idx_subscription_member}), newest first.
 */
public interface MemberSubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** All of one member's subscriptions, most recently started first. */
    List<Subscription> findByMemberIdOrderByStartedAtDesc(Long memberId);
}
