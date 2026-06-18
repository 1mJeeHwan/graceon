package org.streamhub.api.v1.chat.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.chat.entity.ChatIntent;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;

/**
 * One message in the admin thread view (C5): role, content, classified intent (BOT only) and
 * timestamp. Also returned as the payload when an operator posts a manual reply. Mutable to
 * match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRow {

    private Long id;
    private ChatRole role;
    private ChatIntent intent;
    private String content;
    private LocalDateTime createdAt;

    /** Maps a {@link ChatMessage} entity to the admin message row. */
    public static ChatMessageRow from(ChatMessage message) {
        ChatMessageRow row = new ChatMessageRow();
        row.id = message.getId();
        row.role = message.getRole();
        row.intent = message.getIntent();
        row.content = message.getContent();
        row.createdAt = message.getCreatedAt();
        return row;
    }
}
