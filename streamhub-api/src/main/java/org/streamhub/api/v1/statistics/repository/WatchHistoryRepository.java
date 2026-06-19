package org.streamhub.api.v1.statistics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.statistics.entity.WatchHistory;

/** JPA repository for {@link WatchHistory}. Aggregations use MyBatis (StatMapper). */
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    /** A member's watch events, most recent first (paged — the "내 시청기록" feed). */
    Page<WatchHistory> findByMemberIdOrderByWatchedAtDesc(Long memberId, Pageable pageable);
}
