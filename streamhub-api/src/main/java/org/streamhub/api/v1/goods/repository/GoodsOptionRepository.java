package org.streamhub.api.v1.goods.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.goods.entity.GoodsOption;

/** JPA repository for {@link GoodsOption} (per-item option rows, replace-on-save). */
public interface GoodsOptionRepository extends JpaRepository<GoodsOption, Long> {

    List<GoodsOption> findByItemId(Long itemId);

    List<GoodsOption> findByItemIdOrderBySortAscIdAsc(Long itemId);

    void deleteByItemId(Long itemId);
}
