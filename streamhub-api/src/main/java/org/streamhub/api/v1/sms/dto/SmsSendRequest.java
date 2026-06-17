package org.streamhub.api.v1.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin custom SMS send request (C6). The number is masked before storage; nothing is actually
 * dispatched in the demo (mock sender, {@code testMode=Y}).
 *
 * @param toNumber recipient number
 * @param content  message body (1–2000 chars)
 * @param memberId associated member (optional)
 */
public record SmsSendRequest(
        @NotBlank(message = "수신번호는 필수입니다") String toNumber,
        @NotBlank(message = "내용은 필수입니다") @Size(max = 2000, message = "내용은 2000자 이하입니다") String content,
        Long memberId) {
}
