package org.streamhub.api.v1.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat message send request (C5). The {@code sessionKey} is a front-generated UUID kept in
 * localStorage; a new session is created on first use.
 *
 * @param sessionKey conversation key (UUID)
 * @param message    user message (1–2000 chars)
 */
public record ChatSendRequest(
        @NotBlank(message = "세션키는 필수입니다") @Size(max = 40, message = "세션키 길이가 초과되었습니다") String sessionKey,
        @NotBlank(message = "메시지는 필수입니다") @Size(max = 2000, message = "메시지는 2000자 이하입니다") String message) {
}
