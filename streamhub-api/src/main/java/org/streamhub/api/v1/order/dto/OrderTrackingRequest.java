package org.streamhub.api.v1.order.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to set an order's shipment tracking number and carrier. */
public record OrderTrackingRequest(
        @NotBlank String trackingNo,
        String shipCompany) {
}
