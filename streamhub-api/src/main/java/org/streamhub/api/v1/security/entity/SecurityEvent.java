package org.streamhub.api.v1.security.entity;

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
 * A persisted security-monitoring event: an authentication failure, access denial, rate-limit
 * trip, or a derived alert. Written by {@code SecurityMonitor} on the detecting request thread
 * (best-effort — a write failure never breaks the underlying request flow).
 *
 * <p>Unlike {@code ActionLog} (successful operator actions), this records suspicious/abnormal
 * activity for after-the-fact investigation and real-time threshold detection.
 */
@Entity
@Table(name = "SECURITY_EVENT", indexes = {
        @Index(name = "idx_secevent_created", columnList = "created_at"),
        @Index(name = "idx_secevent_type_ip_created", columnList = "event_type, ip, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Event class, e.g. AUTH_FAILURE, ACCESS_DENIED, RATE_LIMIT, SECURITY_ALERT. */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Severity bucket: LOW / MEDIUM / HIGH / CRITICAL. */
    @Column(name = "severity", nullable = false, length = 10)
    private String severity;

    /** Subject class that triggered the event: ADMIN / MEMBER / ANON. */
    @Column(name = "actor_type", length = 10)
    private String actorType;

    /** Identified actor id when known (e.g. matched admin/member), else null. */
    @Column(name = "actor_id")
    private Long actorId;

    /** Login identifier attempted (may be unknown/non-existent), else null. */
    @Column(name = "login_id", length = 100)
    private String loginId;

    /** Originating client IP captured on the detecting request (null off-request). */
    @Column(name = "ip", length = 45)
    private String ip;

    /** Request path involved, when applicable. */
    @Column(name = "path", length = 200)
    private String path;

    /** Free-form context (reason, threshold details). Never carries secrets. */
    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private SecurityEvent(String eventType, String severity, String actorType, Long actorId,
                          String loginId, String ip, String path, String detail,
                          LocalDateTime createdAt) {
        this.eventType = eventType;
        this.severity = severity;
        this.actorType = actorType;
        this.actorId = actorId;
        this.loginId = loginId;
        this.ip = ip;
        this.path = path;
        this.detail = detail;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
