package org.streamhub.api.v1.order.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.order.entity.OrderReceipt;

/** JPA repository for {@link OrderReceipt} (payment/refund records). */
public interface OrderReceiptRepository extends JpaRepository<OrderReceipt, Long> {

    List<OrderReceipt> findByOrderId(Long orderId);

    List<OrderReceipt> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
