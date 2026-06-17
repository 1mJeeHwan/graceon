package org.streamhub.api.v1.order.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.order.entity.OrderItem;

/** JPA repository for {@link OrderItem} (order line snapshots). */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
