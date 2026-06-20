package org.streamhub.api.v1.notification.entity;

/**
 * Audience of a {@link NotificationLog} entry.
 *
 * <p>{@code BROADCAST} is shown to every member; {@code TARGETED} is shown only to the members
 * listed in {@code NOTIFICATION_RECIPIENT}. Stored via {@code @Enumerated(STRING)}; a legacy/seeded
 * row with a null scope is treated as {@code BROADCAST}.
 */
public enum NotificationScope {
    /** 전체 회원 대상. */
    BROADCAST,
    /** 특정 회원 대상(NOTIFICATION_RECIPIENT). */
    TARGETED
}
