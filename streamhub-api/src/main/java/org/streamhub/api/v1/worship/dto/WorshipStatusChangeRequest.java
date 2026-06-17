package org.streamhub.api.v1.worship.dto;

import jakarta.validation.constraints.NotNull;
import org.streamhub.api.v1.worship.entity.RegistrationStatus;

/** Request to transition a registration to a new {@code status}, with an optional admin memo. */
public record WorshipStatusChangeRequest(
        @NotNull RegistrationStatus status,
        String memo) {
}
