package org.streamhub.api.v1.donation.dto;

import jakarta.validation.constraints.NotNull;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;

/** Target state for a subscription lifecycle transition. */
public record SubscriptionStatusRequest(
        @NotNull(message = "전이할 상태는 필수입니다") SubscriptionStatus status) {
}
