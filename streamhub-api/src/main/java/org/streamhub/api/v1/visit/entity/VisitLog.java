package org.streamhub.api.v1.visit.entity;

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
 * A single front-site page hit (접속 통계). One row per visit, with the source IP masked to a
 * /16-style prefix and the user agent parsed into browser / OS / device class at capture time so
 * the stats endpoints can aggregate without re-parsing. All values are demo/fictional (PII guard).
 */
@Entity
@Table(name = "VISIT_LOG", indexes = {
        @Index(name = "idx_visit_visited_at", columnList = "visited_at"),
        @Index(name = "idx_visit_device", columnList = "device_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    /** Source IP masked to its first two octets, e.g. {@code "211.45.*.*"}. */
    @Column(name = "ip_masked", length = 40)
    private String ipMasked;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    /** Parsed browser family: Chrome / Safari / Edge / Firefox / Samsung Internet. */
    @Column(name = "browser", length = 40)
    private String browser;

    /** Parsed OS: Windows / macOS / Android / iOS. */
    @Column(name = "os", length = 40)
    private String os;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    /** Requested front-site path, e.g. {@code /}, {@code /albums}, {@code /churches}. */
    @Column(name = "path", length = 200)
    private String path;

    /** FK → MEMBER for signed-in hits; {@code null} for anonymous visitors. */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private VisitLog(LocalDateTime visitedAt, String ipMasked, String userAgent, String browser,
                     String os, DeviceType deviceType, String path, Long memberId,
                     LocalDateTime createdAt) {
        this.visitedAt = visitedAt;
        this.ipMasked = ipMasked;
        this.userAgent = userAgent;
        this.browser = browser;
        this.os = os;
        this.deviceType = deviceType;
        this.path = path;
        this.memberId = memberId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
