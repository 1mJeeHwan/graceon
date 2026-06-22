package org.streamhub.api.v1.announcement.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.announcement.entity.Announcement;

/** JPA repository for the single-row {@link Announcement} config. */
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** The single announcement row (lowest id), if one has been created. */
    Optional<Announcement> findTopByOrderByIdAsc();
}
