package org.streamhub.api.v1.announcement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.announcement.entity.Announcement;

/** JPA repository for the modal-ad {@link Announcement} rows (managed like banners). */
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** All announcements in display order (sortOrder asc, then id asc). */
    List<Announcement> findAllByOrderBySortOrderAscIdAsc();

    /** Legacy text-only rows (no image) — used by the seeder to migrate to image modal ads. */
    List<Announcement> findByImageUrlIsNull();

    /** Whether any image-based modal ad has been seeded/created yet. */
    boolean existsByImageUrlIsNotNull();
}
