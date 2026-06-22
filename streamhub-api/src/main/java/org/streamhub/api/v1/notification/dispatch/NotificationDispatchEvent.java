package org.streamhub.api.v1.notification.dispatch;

import java.time.LocalDateTime;

/**
 * Cross-service event emitted when the monolith records a notification send. Consumed by the
 * extracted {@code streamhub-notification-service}, which owns the dispatch log in its own DB. Enum
 * fields are carried as their {@code name()} strings so the contract does not depend on the
 * monolith's enum classes (the consumer lives in a different package/service).
 *
 * @param channel      delivery channel name (SMS / PUSH / EMAIL)
 * @param scope        audience name (BROADCAST / TARGETED)
 * @param targetMasked masked recipient summary (PII guard)
 * @param title        notification title
 * @param content      notification body
 * @param status       send result name (SUCCESS / FAIL / PENDING)
 * @param sentAt       send timestamp (null while PENDING)
 */
public record NotificationDispatchEvent(
        String channel,
        String scope,
        String targetMasked,
        String title,
        String content,
        String status,
        LocalDateTime sentAt) {
}
