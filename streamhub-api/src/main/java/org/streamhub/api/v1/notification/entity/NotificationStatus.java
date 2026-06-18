package org.streamhub.api.v1.notification.entity;

/**
 * Delivery result of a {@link NotificationLog} entry (알림센터 발송 로그).
 *
 * <p>Stored via {@code @Enumerated(STRING)}. Log-only demo seam — values are fictional.
 */
public enum NotificationStatus {
    /** 발송 성공. */
    SUCCESS,
    /** 발송 실패. */
    FAIL,
    /** 발송 대기. */
    PENDING
}
