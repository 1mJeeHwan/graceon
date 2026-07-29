package org.streamhub.api.v1.visit.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.visit.entity.VisitLog;

/**
 * JPA repository for {@link VisitLog} (front-site visit statistics).
 *
 * <p>The aggregates below are grouped queries rather than {@code findAll()} plus a Java stream.
 * VISIT_LOG is an append-only traffic log — the one table guaranteed to outgrow every other — so
 * loading it into the heap to count rows scaled the dashboard's memory use with total site traffic
 * and, being inside a read-only transaction, piled every row into the persistence context as well.
 * Counting is what the database is for.
 */
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    List<VisitLog> findByVisitedAtBetween(LocalDateTime from, LocalDateTime to);

    /** Newest-first page of a period, so the row cap is applied by the database, not after loading. */
    List<VisitLog> findByVisitedAtBetweenOrderByVisitedAtDesc(LocalDateTime from, LocalDateTime to,
                                                              Pageable pageable);

    long countByVisitedAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Distinct masked IPs — an approximation of unique visitors, since the stored value is already
     * truncated to a /16. Exact enough at this scale; a HyperLogLog counter would be the next step
     * if the cardinality ever made the {@code COUNT(DISTINCT ...)} too slow.
     */
    @Query("select count(distinct v.ipMasked) from VisitLog v where v.ipMasked is not null")
    long countDistinctIpMasked();

    /** Daily counts for a window, one row per day that actually had traffic (caller zero-fills). */
    @Query("""
            select cast(v.visitedAt as date) as day, count(v)
            from VisitLog v
            where v.visitedAt between :from and :to
            group by cast(v.visitedAt as date)
            order by cast(v.visitedAt as date)
            """)
    List<Object[]> countPerDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Busiest paths, ordered by hit count. The caller applies the top-N limit via {@link Pageable}. */
    @Query("select v.path, count(v) from VisitLog v group by v.path order by count(v) desc")
    List<Object[]> countPerPath(Pageable pageable);

    /** Hits per device type; types with no traffic are simply absent (caller zero-fills). */
    @Query("select v.deviceType, count(v) from VisitLog v where v.deviceType is not null group by v.deviceType")
    List<Object[]> countPerDeviceType();
}
