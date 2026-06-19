package org.streamhub.api.v1.album.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.album.entity.Album;
import org.streamhub.api.v1.pub.me.dto.PurchasedAlbumRow;

/** JPA repository for {@link Album} (CRUD). Listing/search uses MyBatis. */
public interface AlbumRepository extends JpaRepository<Album, Long> {

    /**
     * Albums the member has actually bought, one page at a time: paid orders → line items → the album
     * whose sale-bridge {@code goodsItemId} matches the purchased goods. One row per album (the
     * earliest matching paid order's time wins), most recent purchase first — the "구매 음반" shelf.
     * Post-payment statuses (PAID/READY/SHIPPING/DONE) count; PLACED/CANCEL/RETURN do not (mirrors
     * {@code OrderItemRepository.existsPaidPurchase}). The count query counts distinct purchased
     * albums (the grouped row count), so {@link Page#getTotalElements()} is the album total.
     */
    @Query(value = """
            SELECT new org.streamhub.api.v1.pub.me.dto.PurchasedAlbumRow(
                       a.id, a.title, a.artist, a.coverKey, MIN(o.orderedAt))
            FROM Album a, OrderItem i, Order o
            WHERE i.goodsId = a.goodsItemId
              AND o.id = i.orderId
              AND o.memberId = :memberId
              AND o.status IN (org.streamhub.api.v1.order.entity.OrderStatus.PAID,
                               org.streamhub.api.v1.order.entity.OrderStatus.READY,
                               org.streamhub.api.v1.order.entity.OrderStatus.SHIPPING,
                               org.streamhub.api.v1.order.entity.OrderStatus.DONE)
            GROUP BY a.id, a.title, a.artist, a.coverKey
            ORDER BY MIN(o.orderedAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.id)
            FROM Album a, OrderItem i, Order o
            WHERE i.goodsId = a.goodsItemId
              AND o.id = i.orderId
              AND o.memberId = :memberId
              AND o.status IN (org.streamhub.api.v1.order.entity.OrderStatus.PAID,
                               org.streamhub.api.v1.order.entity.OrderStatus.READY,
                               org.streamhub.api.v1.order.entity.OrderStatus.SHIPPING,
                               org.streamhub.api.v1.order.entity.OrderStatus.DONE)
            """)
    Page<PurchasedAlbumRow> findPurchasedAlbums(@Param("memberId") Long memberId, Pageable pageable);
}
