package org.streamhub.api.v1.chat.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.chat.entity.ChatIntent;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;

/**
 * One message in a chat history reload (C5).
 *
 * @param role      USER / BOT
 * @param intent    classified intent (BOT only)
 * @param content   message text
 * @param createdAt timestamp
 */
public record ChatHistoryItem(
        ChatRole role,
        ChatIntent intent,
        String content,
        LocalDateTime createdAt) {

    /** Maps a {@link ChatMessage} entity to the history DTO. */
    public static ChatHistoryItem from(ChatMessage message) {
        return new ChatHistoryItem(
                message.getRole(),
                message.getIntent(),
                message.getContent(),
                message.getCreatedAt());
    }
}
