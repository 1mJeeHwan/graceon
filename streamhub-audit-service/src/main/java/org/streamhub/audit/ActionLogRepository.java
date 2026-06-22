package org.streamhub.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for {@link ActionLog} — this service's write store for consumed audit events. */
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
}
