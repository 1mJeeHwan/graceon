package org.streamhub.api.v1.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One notification-center send-log row (알림센터 발송 로그). This is a <em>log only</em>:
 * no real SMS/push/email is ever sent — every row is demo/fictional and recipients are
 * masked (PII guard).
 */
@Entity
@Table(name = "NOTIFICATION_LOG", indexes = {
        @Index(name = "idx_notification_channel", columnList = "channel"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    /** Audience: BROADCAST (all members) or TARGETED (NOTIFICATION_RECIPIENT). Null = BROADCAST. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 20)
    private NotificationScope scope;

    /** Masked recipient, e.g. {@code 010-****-1234} or {@code u***@mail.com} (PII guard). */
    @Column(name = "target_masked", nullable = false, length = 120)
    private String targetMasked;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    /** Reason when {@code status == FAIL}; null otherwise. */
    @Column(name = "fail_reason", length = 200)
    private String failReason;

    /** Send timestamp for SUCCESS/FAIL; null while PENDING. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private NotificationLog(NotificationChannel channel, NotificationScope scope, String targetMasked,
                           String title, String content, NotificationStatus status, String failReason,
                           LocalDateTime sentAt, LocalDateTime createdAt) {
        this.channel = channel;
        this.scope = scope != null ? scope : NotificationScope.BROADCAST;
        this.targetMasked = targetMasked;
        this.title = title;
        this.content = content;
        this.status = status;
        this.failReason = failReason;
        this.sentAt = sentAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
