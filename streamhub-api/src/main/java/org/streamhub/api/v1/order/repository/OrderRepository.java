package org.streamhub.api.v1.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.order.entity.Order;

/** JPA repository for {@link Order} (CRUD). Listing/search uses MyBatis. */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);
}
