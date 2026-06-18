package org.streamhub.api.v1.notification.dto;

import java.time.LocalDate;
import org.streamhub.api.v1.notification.entity.NotificationChannel;
import org.streamhub.api.v1.notification.entity.NotificationStatus;

/**
 * Notification-log list search. All filters optional; results are newest first.
 *
 * @param channel  optional channel filter (SMS/PUSH/EMAIL)
 * @param status   optional status filter (SUCCESS/FAIL/PENDING)
 * @param fromDate optional inclusive lower bound on {@code createdAt} (date)
 * @param toDate   optional inclusive upper bound on {@code createdAt} (date)
 * @param keyword  matched against title / content / targetMasked (LIKE)
 */
public record NotificationSearchRequest(
        NotificationChannel channel,
        NotificationStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        String keyword) {
}
