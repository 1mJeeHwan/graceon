package org.streamhub.api.v1.order.dto;

import jakarta.validation.constraints.NotNull;
import org.streamhub.api.v1.order.entity.OrderStatus;

/** Request to transition an order to a new {@code status}, with an optional receipt memo. */
public record OrderStatusChangeRequest(
        @NotNull OrderStatus status,
        String memo) {
}
