package org.streamhub.api.v1.donation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Manual one-off donation entry. */
public record OnceDonationRequest(
        @NotNull(message = "회원은 필수입니다") Long memberId,
        @NotNull(message = "금액은 필수입니다") @Positive(message = "금액은 0보다 커야 합니다") Long amount) {
}
