package org.streamhub.api.v1.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A single message within a {@link ChatSession} (C5). {@code intent} is recorded on BOT replies only. */
@Entity
@Table(name = "CHAT_MESSAGE", indexes = {
        @Index(name = "idx_chat_msg_session", columnList = "session_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → CHAT_SESSION. */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private ChatRole role;

    /** Classified intent (BOT replies only; null for USER messages). */
    @Enumerated(EnumType.STRING)
    @Column(name = "intent", length = 20)
    private ChatIntent intent;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatMessage(Long sessionId, ChatRole role, ChatIntent intent,
                        String content, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.role = role;
        this.intent = intent;
        this.content = content;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
