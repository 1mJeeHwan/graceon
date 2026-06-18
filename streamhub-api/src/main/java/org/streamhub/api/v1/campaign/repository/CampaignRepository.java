package org.streamhub.api.v1.campaign.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.campaign.entity.Campaign;
import org.streamhub.api.v1.campaign.entity.CampaignStatus;
import org.streamhub.api.v1.campaign.entity.CampaignType;

/** JPA repository for {@link Campaign} (campaigns/events). */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByType(CampaignType type);

    List<Campaign> findByStatus(CampaignStatus status);
}
