package org.streamhub.api.v1.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.order.OrderService;
import org.streamhub.api.v1.order.entity.Order;
import org.streamhub.api.v1.order.entity.OrderStatus;
import org.streamhub.api.v1.order.repository.OrderReceiptRepository;
import org.streamhub.api.v1.order.repository.OrderRepository;
import org.streamhub.api.v1.payment.adapter.PaymentProvider;
import org.streamhub.api.v1.payment.adapter.PaymentProviderRouter;
import org.streamhub.api.v1.payment.dto.PayApproveCommand;

/**
 * Unit tests for the C4 payment seam's request→approve txnId consistency: the approve step must
 * present the transaction id issued at the request step (stored on the order). A mismatch is
 * rejected before any provider call (harmless for mock, load-bearing once a real PG is wired in).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderReceiptRepository orderReceiptRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentProviderRouter providerRouter;
    @Mock
    private ActionLogPublisher actionLogPublisher;
    @Mock
    private PaymentProvider paymentProvider;

    private PaymentService paymentService() {
        return new PaymentService(
                orderRepository, orderReceiptRepository, orderService,
                providerRouter, actionLogPublisher, true);
    }

    private Order requestedOrder(String txnId) {
        Order order = Order.builder()
                .orderNo("20260618-000001").memberId(1L).status(OrderStatus.PLACED)
                .orderedName("임지환").receiverName("임지환").goodsTotal(10_000L).total(10_000L)
                .payMethod("CARD").build();
        ReflectionTestUtils.setField(order, "id", 7L);
        order.applyPayRequest("MOCK", txnId); // moves to REQUESTED and stores the issued txnId
        return order;
    }

    @Test
    void approve_txnIdMismatch_isRejectedBeforeProviderCall() {
        Order order = requestedOrder("MOCK-20260618-000001-1");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService().approve(
                new PayApproveCommand(7L, "FORGED-TXN-9999", "4242424242424242")))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_PARAMETER);

        // The mismatch short-circuits before resolving/charging any provider.
        verify(providerRouter, never()).resolve(any());
        verify(paymentProvider, never()).approve(any(), any(), any());
    }
}
