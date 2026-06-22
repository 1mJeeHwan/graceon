package org.streamhub.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Persisted notification-dispatch row owned by this service, in its own schema
 * ({@code streamhub_notification}, DB-per-service). Channel / scope / status are stored as plain
 * strings — this service does not share the monolith's enum classes (cross-service contract). A
 * log-only demo seam: no real SMS/push/email is sent.
 */
@Entity
@Table(name = "NOTIFICATION_DISPATCH", indexes = {
        @Index(name = "idx_dispatch_channel", columnList = "channel"),
        @Index(name = "idx_dispatch_status", columnList = "status"),
        @Index(name = "idx_dispatch_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "scope", length = 20)
    private String scope;

    @Column(name = "target_masked", length = 120)
    private String targetMasked;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private NotificationDispatch(String channel, String scope, String targetMasked, String title,
                                String content, String status, LocalDateTime sentAt,
                                LocalDateTime createdAt) {
        this.channel = channel;
        this.scope = scope;
        this.targetMasked = targetMasked;
        this.title = title;
        this.content = content;
        this.status = status;
        this.sentAt = sentAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
