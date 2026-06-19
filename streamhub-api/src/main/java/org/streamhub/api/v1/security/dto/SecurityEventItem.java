package org.streamhub.api.v1.security.dto;

import java.time.LocalDateTime;
import org.streamhub.api.v1.security.entity.SecurityEvent;

/**
 * One row of the security-event list.
 *
 * @param id        event id
 * @param eventType AUTH_FAILURE / ACCESS_DENIED / RATE_LIMIT / SECURITY_ALERT
 * @param severity  LOW / MEDIUM / HIGH / CRITICAL
 * @param actorType ADMIN / MEMBER / ANON
 * @param actorId   identified actor id, or null
 * @param loginId   attempted login identifier, or null
 * @param ip        originating client IP, or null
 * @param path      request path involved, or null
 * @param detail    free-form context, or null
 * @param createdAt when the event was recorded
 */
public record SecurityEventItem(
        Long id,
        String eventType,
        String severity,
        String actorType,
        Long actorId,
        String loginId,
        String ip,
        String path,
        String detail,
        LocalDateTime createdAt) {

    /** Maps a persisted {@link SecurityEvent} to its list-row view. */
    public static SecurityEventItem from(SecurityEvent event) {
        return new SecurityEventItem(
                event.getId(),
                event.getEventType(),
                event.getSeverity(),
                event.getActorType(),
                event.getActorId(),
                event.getLoginId(),
                event.getIp(),
                event.getPath(),
                event.getDetail(),
                event.getCreatedAt());
    }
}
