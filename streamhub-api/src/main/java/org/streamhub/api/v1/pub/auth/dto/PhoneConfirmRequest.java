package org.streamhub.api.v1.pub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Identity-verification step 2: confirm the SMS authentication code for a phone number. */
public record PhoneConfirmRequest(
        @NotBlank(message = "휴대폰 번호를 입력해 주세요") String phone,
        @NotBlank(message = "인증번호를 입력해 주세요") String code) {
}
