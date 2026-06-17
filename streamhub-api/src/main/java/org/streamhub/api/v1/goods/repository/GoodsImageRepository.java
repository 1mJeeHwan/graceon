package org.streamhub.api.v1.goods.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.goods.entity.GoodsImage;

/** JPA repository for {@link GoodsImage} (per-item gallery rows, replace-on-save). */
public interface GoodsImageRepository extends JpaRepository<GoodsImage, Long> {

    List<GoodsImage> findByItemId(Long itemId);

    List<GoodsImage> findByItemIdOrderBySortAscIdAsc(Long itemId);

    void deleteByItemId(Long itemId);
}
