package org.streamhub.api.v1.security.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.security.entity.SecurityEvent;

/**
 * JPA repository for {@link SecurityEvent}. Writes come from {@code SecurityMonitor};
 * reads serve the admin viewer and the retention/archive job (Phase 3).
 */
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    /**
     * Counts events of one type from a single IP since the given instant — the real-time
     * brute-force threshold check (e.g. AUTH_FAILURE from one IP in the last N minutes).
     */
    long countByEventTypeAndIpAndCreatedAtAfter(String eventType, String ip, LocalDateTime after);

    /** Events older than the cutoff, for the retention archive job (Phase 3). */
    List<SecurityEvent> findByCreatedAtBefore(LocalDateTime cutoff);

    /** Deletes events older than the cutoff after they have been archived (Phase 3 purge). */
    @Modifying
    @Transactional
    @Query("delete from SecurityEvent e where e.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
