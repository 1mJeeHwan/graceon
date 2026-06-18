package org.streamhub.api.v1.goods.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.goods.entity.GoodsItem;

/**
 * Stock-management JPA repository for {@link GoodsItem}. Defined separately from the
 * existing goods repositories so the stock module owns its own data access.
 */
public interface GoodsStockRepository extends JpaRepository<GoodsItem, Long> {
}
