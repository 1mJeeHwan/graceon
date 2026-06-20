package org.streamhub.api.v1.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.notification.entity.NotificationRecipient;

/** JPA repository for {@link NotificationRecipient} (targeted-notification fan-out rows). */
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
}
