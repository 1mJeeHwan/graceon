package org.streamhub.api.v1.notification.dispatch;

/**
 * Seam for emitting {@link NotificationDispatchEvent}s to the extracted notification service. Mirrors
 * the action-log emitter seam:
 *
 * <ul>
 *   <li>{@link NoopNotificationDispatcher} (default) — does nothing, so the monolith's notification
 *       domain runs standalone with no broker dependency.</li>
 *   <li>{@link KafkaNotificationDispatcher} ({@code app.notification.dispatch.enabled=true}) —
 *       publishes the event to Kafka for {@code streamhub-notification-service} to consume and persist
 *       in its own DB (DB-per-service). Best-effort: a messaging failure never breaks the send.</li>
 * </ul>
 */
public interface NotificationDispatcher {

    /** Emits one notification-dispatch event (best-effort). */
    void dispatch(NotificationDispatchEvent event);
}
