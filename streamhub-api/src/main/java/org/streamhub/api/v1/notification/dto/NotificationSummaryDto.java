package org.streamhub.api.v1.notification.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate counts for the notification-center dashboard (알림센터 발송 로그 요약).
 * Demo/fictional — derived purely from the seeded log rows. {@code byChannel} holds the
 * per-channel totals keyed {@code SMS}/{@code PUSH}/{@code EMAIL}.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificationSummaryDto {
    private long total;
    private long successCount;
    private long failCount;
    private long pendingCount;
    private Map<String, Long> byChannel = new LinkedHashMap<>();
}
