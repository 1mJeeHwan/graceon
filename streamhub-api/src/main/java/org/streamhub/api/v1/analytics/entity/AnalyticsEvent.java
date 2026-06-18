package org.streamhub.api.v1.analytics.entity;

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
 * A single web-analytics event posted by the user site (Firebase-style ingest). One row per
 * client action — a page view, a content view (with dwell time) or a session start. The content
 * title is denormalized onto the row so the admin dashboards can rank content without joining back
 * to the album/video/post domains. All values are demo/fictional (PII guard).
 */
@Entity
@Table(name = "ANALYTICS_EVENT", indexes = {
        @Index(name = "idx_analytics_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_analytics_content_type", columnList = "content_type"),
        @Index(name = "idx_analytics_target_id", columnList = "target_id"),
        @Index(name = "idx_analytics_session_id", columnList = "session_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentKind contentType;

    /** Id of the viewed content; {@code null} for plain page views. */
    @Column(name = "target_id")
    private Long targetId;

    /** Denormalized content title sent by the client; {@code null} for plain page views. */
    @Column(name = "title", length = 200)
    private String title;

    /** The client route, e.g. {@code /albums/2}. */
    @Column(name = "path", length = 200)
    private String path;

    /** Client-generated session id (groups events from one browsing session). */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /** FK → MEMBER for signed-in events; {@code null} for anonymous visitors. */
    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceKind deviceType;

    @Column(name = "referrer", length = 300)
    private String referrer;

    /** Time spent on the content/page in milliseconds (sent on leave); {@code null} otherwise. */
    @Column(name = "dwell_ms")
    private Long dwellMs;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AnalyticsEvent(EventType type, ContentKind contentType, Long targetId, String title,
                          String path, String sessionId, Long memberId, DeviceKind deviceType,
                          String referrer, Long dwellMs, LocalDateTime occurredAt,
                          LocalDateTime createdAt) {
        this.type = type;
        this.contentType = contentType;
        this.targetId = targetId;
        this.title = title;
        this.path = path;
        this.sessionId = sessionId;
        this.memberId = memberId;
        this.deviceType = deviceType;
        this.referrer = referrer;
        this.dwellMs = dwellMs;
        this.occurredAt = occurredAt != null ? occurredAt : LocalDateTime.now();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
