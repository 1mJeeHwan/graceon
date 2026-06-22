package org.streamhub.api.v1.actionlog.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Store for {@link ActionOutbox} rows: the relay claims unpublished rows and flips their state. */
public interface ActionOutboxRepository extends JpaRepository<ActionOutbox, Long> {

    /** Oldest-first batch of not-yet-published rows for the relay to drain. */
    List<ActionOutbox> findTop100ByPublishedFalseOrderByIdAsc();

    @Modifying
    @Transactional
    @Query("update ActionOutbox o set o.published = true, o.publishedAt = :at where o.id in :ids")
    void markPublished(@Param("ids") List<Long> ids, @Param("at") LocalDateTime at);

    @Modifying
    @Transactional
    @Query("update ActionOutbox o set o.attempts = o.attempts + 1 where o.id in :ids")
    void recordFailure(@Param("ids") List<Long> ids);
}
