package org.streamhub.api.v1.media.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.media.entity.MediaAsset;

/** JPA repository for {@link MediaAsset} — paginated search + the category facet for the library. */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    @Query("SELECT m FROM MediaAsset m "
            + "WHERE (:category IS NULL OR m.category = :category) "
            + "AND (:keyword IS NULL OR LOWER(m.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<MediaAsset> search(
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT DISTINCT m.category FROM MediaAsset m ORDER BY m.category")
    List<String> findDistinctCategories();
}
