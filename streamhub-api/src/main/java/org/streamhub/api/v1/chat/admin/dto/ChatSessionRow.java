package org.streamhub.api.v1.chat.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.chat.entity.ChatIntent;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;
import org.streamhub.api.v1.chat.entity.ChatSession;

/**
 * One row in the admin chat-session console list (C5). Summarizes a session by its last
 * message: a short snippet, the message count, and an {@code unanswered} flag (true when the
 * last turn is from the USER side, i.e. still awaiting an operator/bot reply). All values are
 * demo/fictional (PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatSessionRow {

    private static final int SNIPPET_MAX = 60;

    private String sessionKey;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long messageCount;
    private boolean unanswered;
    private ChatIntent intent;

    /**
     * Builds a list row from a session and its last message.
     *
     * @param session      the chat session
     * @param lastMessage  the most recent message, or {@code null} for an empty session
     * @param messageCount total messages in the session
     */
    public static ChatSessionRow from(ChatSession session, ChatMessage lastMessage, long messageCount) {
        ChatSessionRow row = new ChatSessionRow();
        row.sessionKey = session.getSessionKey();
        row.messageCount = messageCount;
        if (lastMessage != null) {
            row.lastMessage = snippet(lastMessage.getContent());
            row.lastMessageAt = lastMessage.getCreatedAt();
            row.unanswered = lastMessage.getRole() == ChatRole.USER;
            row.intent = lastMessage.getIntent();
        }
        return row;
    }

    private static String snippet(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= SNIPPET_MAX ? content : content.substring(0, SNIPPET_MAX) + "…";
    }
}
