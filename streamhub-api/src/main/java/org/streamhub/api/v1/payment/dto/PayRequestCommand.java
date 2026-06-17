package org.streamhub.api.v1.payment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payment initiation request. The amount is never accepted from the client — it is always the
 * server-computed {@code order.total} (spec §3.5).
 *
 * @param orderId  target order id
 * @param provider PG code to attempt ({@code MOCK}/{@code TOSS}/{@code PAYPAL}/{@code KAKAO}/{@code CARD}); null = default
 */
public record PayRequestCommand(
        @NotNull(message = "주문은 필수입니다") Long orderId,
        String provider) {
}
