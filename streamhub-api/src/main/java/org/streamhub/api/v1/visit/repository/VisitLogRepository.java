package org.streamhub.api.v1.visit.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.visit.entity.VisitLog;

/** JPA repository for {@link VisitLog} (front-site visit statistics). */
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    List<VisitLog> findByVisitedAtBetween(LocalDateTime from, LocalDateTime to);
}
