package org.streamhub.api.v1.notification.entity;

/**
 * Delivery channel of a {@link NotificationLog} entry (알림센터 발송 로그).
 *
 * <p>Stored via {@code @Enumerated(STRING)}. This is a log-only demo seam — no real
 * message is ever sent.
 */
public enum NotificationChannel {
    /** 문자(SMS). */
    SMS,
    /** 앱 푸시. */
    PUSH,
    /** 이메일. */
    EMAIL
}
