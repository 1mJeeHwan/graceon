package org.streamhub.api.v1.goods.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.goods.entity.GoodsItem;

/** JPA repository for {@link GoodsItem} (CRUD). Listing/search uses MyBatis. */
public interface GoodsItemRepository extends JpaRepository<GoodsItem, Long> {

    /** Bulk lookup for inline grid edits ({@code MemberRepository.findAllByIdIn} pattern). */
    List<GoodsItem> findAllByIdIn(List<Long> ids);

    long countByCategoryId(Long categoryId);

    /** Public storefront detail: an on-sale ({@code use_yn='Y'}) item by id. */
    Optional<GoodsItem> findByIdAndUseYn(Long id, String useYn);

    /** Public storefront listing: all on-sale items, best-selling then newest first. */
    Page<GoodsItem> findByUseYnOrderBySaleCountDescCreatedAtDescIdDesc(String useYn, Pageable pageable);

    /** Public storefront search: on-sale items whose name matches, best-selling then newest first. */
    Page<GoodsItem> findByUseYnAndNameContainingIgnoreCaseOrderBySaleCountDescCreatedAtDescIdDesc(
            String useYn, String name, Pageable pageable);

    /**
     * Atomically deducts item stock and bumps the sale count in a single guarded UPDATE,
     * eliminating the read-modify-write race that allowed oversell. The {@code stock >= qty}
     * guard means a losing concurrent checkout simply affects 0 rows (caller treats that as
     * out-of-stock) instead of driving stock negative. The {@code @Version} column is bumped
     * by hand because a JPQL bulk UPDATE does not increment it automatically.
     *
     * @return rows affected — {@code 1} on success, {@code 0} if stock was insufficient
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GoodsItem g SET g.stock = g.stock - :qty, g.saleCount = g.saleCount + :qty, "
            + "g.version = g.version + 1, g.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE g.id = :id AND g.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Atomically restores item stock and walks the sale count back (floored at 0) on
     * cancel/return. The symmetric counterpart to {@link #decrementStock}; no lower-bound
     * guard is needed since adding stock can never underflow.
     *
     * @return rows affected — {@code 1} if the item exists, otherwise {@code 0}
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GoodsItem g SET g.stock = g.stock + :qty, "
            + "g.saleCount = CASE WHEN g.saleCount - :qty < 0 THEN 0 ELSE g.saleCount - :qty END, "
            + "g.version = g.version + 1, g.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE g.id = :id")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);
}
