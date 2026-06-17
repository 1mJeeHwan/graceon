package org.streamhub.api.v1.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Manual point grant/deduction request (gnuboard 수동 지급 폼).
 *
 * @param memberId   target member
 * @param delta      signed amount; negative deducts/uses points
 * @param reason     reason recorded on the ledger entry
 * @param expireDays validity in days; {@code null} means the accrual never expires
 */
public record PointGrantRequest(
        @NotNull(message = "회원을 선택하세요") Long memberId,
        @NotNull(message = "포인트를 입력하세요") Long delta,
        @NotBlank(message = "사유를 입력하세요") String reason,
        Integer expireDays) {
}
