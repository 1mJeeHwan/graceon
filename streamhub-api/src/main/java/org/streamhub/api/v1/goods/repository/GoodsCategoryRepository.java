package org.streamhub.api.v1.goods.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.goods.entity.GoodsCategory;

/** JPA repository for {@link GoodsCategory}. The category tree is read in full. */
public interface GoodsCategoryRepository extends JpaRepository<GoodsCategory, Long> {

    List<GoodsCategory> findAllByOrderBySortAscIdAsc();

    List<GoodsCategory> findByParentId(Long parentId);
}
