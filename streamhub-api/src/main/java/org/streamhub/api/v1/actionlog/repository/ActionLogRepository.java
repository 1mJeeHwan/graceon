package org.streamhub.api.v1.actionlog.repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.actionlog.entity.ActionLog;

/** JPA repository for {@link ActionLog} (writes from the SQS consumer + seed). */
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {

    /** Rows older than {@code cutoff}, read for the weekly archive-then-purge job. */
    List<ActionLog> findByCreatedAtBefore(LocalDateTime cutoff);

    /**
     * One id-ordered chunk of rows older than the cutoff, for the weekly archive. Paged because the
     * archiver used to load every matching row at once and hold it three times over — as entities,
     * as one giant StringBuilder, and as the byte array handed to S3.
     */
    List<ActionLog> findByCreatedAtBeforeOrderByIdAsc(LocalDateTime cutoff, Pageable pageable);

    /** Bulk-deletes rows older than {@code cutoff} after a successful archive upload. */
    @Modifying
    @Query("delete from ActionLog a where a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
