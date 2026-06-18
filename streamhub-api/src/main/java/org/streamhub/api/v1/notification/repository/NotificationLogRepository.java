package org.streamhub.api.v1.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.notification.entity.NotificationLog;

/** JPA repository for {@link NotificationLog} (알림센터 발송 로그). */
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
