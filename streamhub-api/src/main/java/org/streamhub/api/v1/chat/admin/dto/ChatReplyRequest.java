package org.streamhub.api.v1.chat.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manual operator reply input (C5). The admin appends a message to an existing session; the
 * reply is stored as a {@code BOT} turn (the assistant/operator side). Mutable to match the
 * project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatReplyRequest {

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 2000, message = "내용은 2000자 이하입니다")
    private String content;
}
