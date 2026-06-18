package org.streamhub.api.v1.analytics.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.analytics.entity.AnalyticsEvent;

/** JPA repository for {@link AnalyticsEvent} (web-analytics events). */
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    List<AnalyticsEvent> findByOccurredAtBetween(LocalDateTime from, LocalDateTime to);
}
