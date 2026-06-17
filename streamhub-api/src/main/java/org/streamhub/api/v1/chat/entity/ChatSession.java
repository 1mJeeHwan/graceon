package org.streamhub.api.v1.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A chatbot conversation session (C5). Keyed by a front-generated UUID stored in localStorage;
 * anonymous (no member) sessions are allowed for the public widget.
 */
@Entity
@Table(name = "CHAT_SESSION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Front-generated UUID ({@code crypto.randomUUID()}); unique. */
    @Column(name = "session_key", nullable = false, unique = true, length = 40)
    private String sessionKey;

    /** Associated member when logged in (nullable for anonymous widget use). */
    @Column(name = "member_id")
    private Long memberId;

    /** Provider that handled the session ({@code RULE}/{@code LLM}). */
    @Column(name = "provider", nullable = false, length = 10)
    private String provider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatSession(String sessionKey, Long memberId, String provider, LocalDateTime createdAt) {
        this.sessionKey = sessionKey;
        this.memberId = memberId;
        this.provider = provider != null ? provider : "RULE";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
