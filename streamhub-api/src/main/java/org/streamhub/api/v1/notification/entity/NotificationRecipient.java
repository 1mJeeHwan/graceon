package org.streamhub.api.v1.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Recipient row of a {@code TARGETED} {@link NotificationLog} — links one notification to one
 * member. A notification with no recipient rows is {@code BROADCAST} (every member). The
 * {@code (notification_id, member_id)} unique constraint keeps the fan-out idempotent.
 */
@Entity
@Table(name = "NOTIFICATION_RECIPIENT",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_recipient",
                columnNames = {"notification_id", "member_id"}),
        indexes = {
                @Index(name = "idx_notification_recipient_member", columnList = "member_id"),
                @Index(name = "idx_notification_recipient_notification", columnList = "notification_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder
    private NotificationRecipient(Long notificationId, Long memberId) {
        this.notificationId = notificationId;
        this.memberId = memberId;
    }
}
