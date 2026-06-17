package org.streamhub.api.v1.donation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.donation.entity.Donation;

/** JPA repository for {@link Donation}. Listing/filtering uses MyBatis. */
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findBySubscriptionId(Long subscriptionId);

    List<Donation> findByMemberId(Long memberId);
}
