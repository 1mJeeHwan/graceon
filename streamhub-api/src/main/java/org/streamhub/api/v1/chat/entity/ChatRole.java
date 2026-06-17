package org.streamhub.api.v1.chat.entity;

/** Author of a chat message (C5). Stored via {@code @Enumerated(STRING)}. */
public enum ChatRole {
    /** 사용자 입력. */
    USER,
    /** 봇 응답. */
    BOT
}
