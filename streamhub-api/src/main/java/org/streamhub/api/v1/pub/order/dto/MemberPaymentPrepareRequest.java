package org.streamhub.api.v1.pub.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Phase 1 of a real-PG album purchase: prepare an order before opening the payment window.
 *
 * @param albumId  album to buy
 * @param provider PG code (e.g. {@code TOSS}); defaults to TOSS when blank
 */
public record MemberPaymentPrepareRequest(
        @NotNull(message = "앨범을 선택하세요") Long albumId,
        String provider) {
}
