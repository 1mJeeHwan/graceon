package org.streamhub.api.v1.order.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.order.entity.Order;

/** JPA repository for {@link Order} (CRUD). Listing/search uses MyBatis. */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    boolean existsByOrderNo(String orderNo);

    /** A member's own orders, newest first (public "구매내역"). */
    List<Order> findByMemberIdOrderByOrderedAtDescIdDesc(Long memberId);
}
