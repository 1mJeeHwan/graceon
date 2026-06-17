package org.streamhub.api.v1.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payment approval (confirm) request. The card number is <b>never stored</b> — only a masked
 * form is written to the receipt memo (PCI avoidance, demo; spec §3.5).
 *
 * @param orderId target order id
 * @param txnId   transaction id returned by {@code /v1/payment/request}
 * @param cardNo  test card number (e.g. {@code 4242 4242 4242 4242}); masked then discarded; may be null
 */
public record PayApproveCommand(
        @NotNull(message = "주문은 필수입니다") Long orderId,
        @NotBlank(message = "거래번호는 필수입니다") String txnId,
        String cardNo) {
}
