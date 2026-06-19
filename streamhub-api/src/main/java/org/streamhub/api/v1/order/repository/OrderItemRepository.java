package org.streamhub.api.v1.order.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.streamhub.api.v1.order.entity.OrderItem;

/** JPA repository for {@link OrderItem} (order line snapshots). */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    /**
     * Whether the member owns a paid order containing the given goods item — i.e. has purchased it.
     * Gates encrypted full-track playback (the HLS key) to actual buyers. Post-payment statuses
     * (PAID/READY/SHIPPING/DONE) count; PLACED/CANCEL/RETURN do not.
     */
    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM OrderItem i, Order o
            WHERE o.id = i.orderId
              AND o.memberId = :memberId
              AND i.goodsId = :goodsId
              AND o.status IN (org.streamhub.api.v1.order.entity.OrderStatus.PAID,
                               org.streamhub.api.v1.order.entity.OrderStatus.READY,
                               org.streamhub.api.v1.order.entity.OrderStatus.SHIPPING,
                               org.streamhub.api.v1.order.entity.OrderStatus.DONE)
            """)
    boolean existsPaidPurchase(@Param("memberId") Long memberId, @Param("goodsId") Long goodsId);
}
