package org.streamhub.api.v1.pub.me.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.notification.entity.NotificationLog;
import org.streamhub.api.v1.notification.entity.NotificationStatus;

/**
 * Read-side repository for the member notification feed. Kept separate from the admin
 * {@code NotificationLogRepository} so the public {@code /pub/v1/me} feature owns its own query
 * surface. {@code NOTIFICATION_LOG} is a broadcast send-log with no member targeting, so the feed
 * is simply the most recent successfully-sent notifications, newest first.
 */
public interface MemberNotificationRepository extends JpaRepository<NotificationLog, Long> {

    /** Most recent successfully-sent broadcast notifications, newest first. */
    Page<NotificationLog> findByStatusOrderByCreatedAtDesc(NotificationStatus status, Pageable pageable);
}
