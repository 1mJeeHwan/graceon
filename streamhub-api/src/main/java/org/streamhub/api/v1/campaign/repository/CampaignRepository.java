package org.streamhub.api.v1.campaign.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.campaign.entity.Campaign;
import org.streamhub.api.v1.campaign.entity.CampaignStatus;
import org.streamhub.api.v1.campaign.entity.CampaignType;

/** JPA repository for {@link Campaign} (campaigns/events). */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByType(CampaignType type);

    List<Campaign> findByStatus(CampaignStatus status);

    /** Public listing: a single lifecycle status (e.g. {@code ACTIVE}), newest-starting first. */
    Page<Campaign> findByStatusOrderByStartAtDescIdDesc(CampaignStatus status, Pageable pageable);
}
