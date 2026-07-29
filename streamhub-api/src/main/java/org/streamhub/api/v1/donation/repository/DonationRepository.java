package org.streamhub.api.v1.donation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.donation.entity.Donation;

/** JPA repository for {@link Donation}. Listing/filtering uses MyBatis. */
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findBySubscriptionId(Long subscriptionId);

    List<Donation> findByMemberId(Long memberId);

    /**
     * Whether this subscription cycle has already been charged. Checked before inserting so a
     * repeated billing run returns without poisoning the transaction; the {@code uk_donation_cycle}
     * unique constraint remains the authority for genuinely concurrent attempts.
     */
    boolean existsBySubscriptionIdAndCycleNo(Long subscriptionId, Integer cycleNo);
}
