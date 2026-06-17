package org.streamhub.api.v1.goods.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.goods.entity.GoodsItem;

/** JPA repository for {@link GoodsItem} (CRUD). Listing/search uses MyBatis. */
public interface GoodsItemRepository extends JpaRepository<GoodsItem, Long> {

    /** Bulk lookup for inline grid edits ({@code MemberRepository.findAllByIdIn} pattern). */
    List<GoodsItem> findAllByIdIn(List<Long> ids);

    long countByCategoryId(Long categoryId);
}
