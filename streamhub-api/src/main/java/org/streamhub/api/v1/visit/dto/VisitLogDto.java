package org.streamhub.api.v1.visit.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.visit.entity.DeviceType;
import org.streamhub.api.v1.visit.entity.VisitLog;

/**
 * A single visit-log row for the admin list view. Mutable to match the project DTO style. All
 * values are demo/fictional (the IP is already masked at capture time).
 */
@Getter
@Setter
@NoArgsConstructor
public class VisitLogDto {
    private Long id;
    private LocalDateTime visitedAt;
    private String ipMasked;
    private String userAgent;
    private String browser;
    private String os;
    private DeviceType deviceType;
    private String path;
    private Long memberId;

    /** Builds a DTO from a persisted visit log. */
    public static VisitLogDto from(VisitLog log) {
        VisitLogDto dto = new VisitLogDto();
        dto.id = log.getId();
        dto.visitedAt = log.getVisitedAt();
        dto.ipMasked = log.getIpMasked();
        dto.userAgent = log.getUserAgent();
        dto.browser = log.getBrowser();
        dto.os = log.getOs();
        dto.deviceType = log.getDeviceType();
        dto.path = log.getPath();
        dto.memberId = log.getMemberId();
        return dto;
    }
}
