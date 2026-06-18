package org.streamhub.api.v1.notification.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.notification.entity.NotificationChannel;
import org.streamhub.api.v1.notification.entity.NotificationLog;
import org.streamhub.api.v1.notification.entity.NotificationStatus;

/**
 * A notification-center send-log row (알림센터 발송 로그). Read-only output for the
 * list/detail endpoints. All values are demo/fictional and recipients are masked
 * (PII guard) — this is a log only, nothing is ever actually sent. Mutable to match
 * the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificationLogDto {
    private Long id;
    private NotificationChannel channel;
    private String targetMasked;
    private String title;
    private String content;
    private NotificationStatus status;
    private String failReason;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted log row. */
    public static NotificationLogDto from(NotificationLog log) {
        NotificationLogDto dto = new NotificationLogDto();
        dto.id = log.getId();
        dto.channel = log.getChannel();
        dto.targetMasked = log.getTargetMasked();
        dto.title = log.getTitle();
        dto.content = log.getContent();
        dto.status = log.getStatus();
        dto.failReason = log.getFailReason();
        dto.sentAt = log.getSentAt();
        dto.createdAt = log.getCreatedAt();
        return dto;
    }
}
