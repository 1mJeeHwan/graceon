package org.streamhub.notification;

import java.time.LocalDateTime;

/**
 * The dispatch event payload as published by the monolith ({@code streamhub-api}'s
 * {@code NotificationDispatchEvent}). This is the cross-service contract — kept structurally
 * identical to the producer's record so JSON (de)serialization lines up. Enum-typed fields arrive as
 * their {@code name()} strings.
 */
public record NotificationDispatchMessage(
        String channel,
        String scope,
        String targetMasked,
        String title,
        String content,
        String status,
        LocalDateTime sentAt) {
}
