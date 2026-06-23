package org.streamhub.api.v1.pub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Identity-verification step 1: request an SMS authentication code for a phone number.
 * {@code carrier} mirrors the real 통신사 본인인증 UX (SKT/KT/LGU+/알뜰폰); it is not persisted.
 */
public record PhoneVerifyRequest(
        @NotBlank(message = "이름을 입력해 주세요") String name,
        @NotBlank(message = "통신사를 선택해 주세요") String carrier,
        @NotBlank(message = "휴대폰 번호를 입력해 주세요")
        @Pattern(regexp = "^01[016789][-]?\\d{3,4}[-]?\\d{4}$", message = "올바른 휴대폰 번호를 입력해 주세요")
        String phone) {
}
