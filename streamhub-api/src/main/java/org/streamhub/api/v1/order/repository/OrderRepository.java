package org.streamhub.api.v1.order.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.order.entity.Order;
import org.streamhub.api.v1.order.entity.OrderStatus;

/** JPA repository for {@link Order} (CRUD). Listing/search uses MyBatis. */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    boolean existsByOrderNo(String orderNo);

    /** A member's own orders, newest first, one page at a time (public "구매내역"). */
    Page<Order> findByMemberIdOrderByOrderedAtDescIdDesc(Long memberId, Pageable pageable);

    /** Orders in a given status (used by the delivery-sync scheduler to poll in-transit orders). */
    List<Order> findByStatus(OrderStatus status);
}
