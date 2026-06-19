package org.streamhub.api.v1.pub.me.notification.dto;

import java.time.LocalDateTime;

/**
 * One notification-center item exposed to a logged-in member.
 *
 * <p>The backing {@code NOTIFICATION_LOG} is a <em>broadcast send-log</em> with no per-member
 * targeting or per-member read state, so {@code read} is always {@code false} and the feed shows
 * the most recent successfully-sent broadcasts to every member alike.
 *
 * @param id        notification log id
 * @param title     notification title
 * @param body      notification body (maps to the log's {@code content}; may be null)
 * @param read      always {@code false} — the broadcast log has no per-member read state
 * @param createdAt when the notification was created
 */
public record NotificationItem(
        Long id,
        String title,
        String body,
        boolean read,
        LocalDateTime createdAt) {
}
