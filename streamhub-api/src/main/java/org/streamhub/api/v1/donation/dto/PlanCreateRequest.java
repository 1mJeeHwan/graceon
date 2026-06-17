package org.streamhub.api.v1.donation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.streamhub.api.v1.member.entity.MemberGrade;

/**
 * Create/update payload for a membership plan.
 */
public record PlanCreateRequest(
        @NotBlank(message = "플랜명을 입력하세요") String name,
        @NotNull(message = "등급은 필수입니다") MemberGrade grade,
        @NotNull(message = "금액은 필수입니다") @Positive(message = "금액은 0보다 커야 합니다") Long price,
        @NotNull(message = "청구 주기는 필수입니다") @Positive(message = "청구 주기는 0보다 커야 합니다") Integer periodMonths,
        @NotNull(message = "적립률은 필수입니다") @Min(value = 0, message = "적립률은 0 이상이어야 합니다")
        @Max(value = 100, message = "적립률은 100 이하여야 합니다") Integer pointRate,
        String benefit,
        @NotNull(message = "활성 여부는 필수입니다") String active) {
}
