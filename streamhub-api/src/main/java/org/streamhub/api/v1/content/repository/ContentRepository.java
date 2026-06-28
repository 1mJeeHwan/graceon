package org.streamhub.api.v1.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.content.entity.Content;

/** JPA repository for {@link Content} (CRUD). Listing/search uses MyBatis. */
public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
     * Atomically increments the view count of a PUBLISHED content (avoids the read-modify-write
     * lost-update race when many viewers open the same content concurrently). Returns the number of
     * rows updated (0 when the id is missing or not published) so callers know whether it counted.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Content c SET c.viewCount = c.viewCount + 1 "
            + "WHERE c.id = :id AND c.status = org.streamhub.api.v1.content.entity.ContentStatus.PUBLISHED")
    int incrementViewCount(@Param("id") Long id);
}
