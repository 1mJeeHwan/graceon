package org.streamhub.notification;

import java.time.LocalDateTime;

/** One dispatch row as returned by this service's read API. */
public record NotificationDispatchView(
        Long id,
        String channel,
        String scope,
        String targetMasked,
        String title,
        String content,
        String status,
        LocalDateTime sentAt,
        LocalDateTime createdAt) {

    static NotificationDispatchView from(NotificationDispatch entity) {
        return new NotificationDispatchView(
                entity.getId(),
                entity.getChannel(),
                entity.getScope(),
                entity.getTargetMasked(),
                entity.getTitle(),
                entity.getContent(),
                entity.getStatus(),
                entity.getSentAt(),
                entity.getCreatedAt());
    }
}
