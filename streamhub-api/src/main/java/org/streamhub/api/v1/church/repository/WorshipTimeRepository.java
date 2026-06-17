package org.streamhub.api.v1.church.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.church.entity.WorshipTime;

/** JPA repository for {@link WorshipTime} (church worship schedules). */
public interface WorshipTimeRepository extends JpaRepository<WorshipTime, Long> {

    List<WorshipTime> findByChurchIdOrderBySortAscIdAsc(Long churchId);

    void deleteByChurchId(Long churchId);
}
