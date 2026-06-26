package org.streamhub.api.v1.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat message send request (C5). The {@code sessionKey} + {@code sessionToken} are issued by the
 * server on the first send and cached client-side; the client replays them to continue the session.
 * Both are optional/blank on the very first message — the server then issues a fresh session.
 *
 * @param sessionKey   server-issued session id to continue (blank → new session)
 * @param sessionToken server-issued secret proving ownership of {@code sessionKey}
 * @param message      user message (1–2000 chars)
 */
public record ChatSendRequest(
        @Size(max = 40, message = "세션키 길이가 초과되었습니다") String sessionKey,
        @Size(max = 64, message = "세션 토큰 길이가 초과되었습니다") String sessionToken,
        @NotBlank(message = "메시지는 필수입니다") @Size(max = 2000, message = "메시지는 2000자 이하입니다") String message) {
}
