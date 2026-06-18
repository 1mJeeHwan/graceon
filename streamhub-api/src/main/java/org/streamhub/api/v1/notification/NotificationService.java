package org.streamhub.api.v1.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.notification.dto.NotificationLogDto;
import org.streamhub.api.v1.notification.dto.NotificationSearchRequest;
import org.streamhub.api.v1.notification.dto.NotificationSummaryDto;
import org.streamhub.api.v1.notification.entity.NotificationChannel;
import org.streamhub.api.v1.notification.entity.NotificationLog;
import org.streamhub.api.v1.notification.entity.NotificationStatus;
import org.streamhub.api.v1.notification.repository.NotificationLogRepository;

/**
 * Notification-center send-log (알림센터 발송 로그). This is a <em>log only</em> seam: no
 * real SMS/push/email is sent — the dataset is seeded demo data. The dataset is small,
 * so listing/summary load all rows and filter/aggregate in memory.
 */
@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final ActionLogPublisher actionLogPublisher;

    public NotificationService(NotificationLogRepository notificationLogRepository,
                               ActionLogPublisher actionLogPublisher) {
        this.notificationLogRepository = notificationLogRepository;
        this.actionLogPublisher = actionLogPublisher;
    }

    /** Filtered log listing, newest first. */
    @Transactional(readOnly = true)
    public List<NotificationLogDto> list(NotificationSearchRequest request) {
        return notificationLogRepository.findAll().stream()
                .filter(log -> matches(log, request))
                .sorted(Comparator.comparing(NotificationLog::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparing(NotificationLog::getId).reversed()))
                .map(NotificationLogDto::from)
                .toList();
    }

    /** Detail lookup. */
    @Transactional(readOnly = true)
    public NotificationLogDto detail(Long id) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return NotificationLogDto.from(log);
    }

    /** Aggregate counts across the whole log (status totals + per-channel breakdown). */
    @Transactional(readOnly = true)
    public NotificationSummaryDto summary() {
        List<NotificationLog> logs = notificationLogRepository.findAll();
        NotificationSummaryDto dto = new NotificationSummaryDto();
        dto.setTotal(logs.size());
        dto.setSuccessCount(countStatus(logs, NotificationStatus.SUCCESS));
        dto.setFailCount(countStatus(logs, NotificationStatus.FAIL));
        dto.setPendingCount(countStatus(logs, NotificationStatus.PENDING));
        dto.getByChannel().put(NotificationChannel.SMS.name(), countChannel(logs, NotificationChannel.SMS));
        dto.getByChannel().put(NotificationChannel.PUSH.name(), countChannel(logs, NotificationChannel.PUSH));
        dto.getByChannel().put(NotificationChannel.EMAIL.name(), countChannel(logs, NotificationChannel.EMAIL));
        return dto;
    }

    /** Purges a single log row. */
    @Transactional
    public void delete(Long id) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        notificationLogRepository.delete(log);
        actionLogPublisher.publish("NOTIFICATION_DELETE", "NOTIFICATION", String.valueOf(id), log.getTitle());
    }

    // --- helpers -----------------------------------------------------------

    private boolean matches(NotificationLog log, NotificationSearchRequest request) {
        if (request == null) {
            return true;
        }
        if (request.channel() != null && log.getChannel() != request.channel()) {
            return false;
        }
        if (request.status() != null && log.getStatus() != request.status()) {
            return false;
        }
        LocalDateTime createdAt = log.getCreatedAt();
        if (request.fromDate() != null && createdAt.isBefore(request.fromDate().atStartOfDay())) {
            return false;
        }
        if (request.toDate() != null && createdAt.isAfter(endOfDay(request.toDate()))) {
            return false;
        }
        return keywordMatches(log, request.keyword());
    }

    private boolean keywordMatches(NotificationLog log, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.toLowerCase();
        return contains(log.getTitle(), needle)
                || contains(log.getContent(), needle)
                || contains(log.getTargetMasked(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    private long countStatus(List<NotificationLog> logs, NotificationStatus status) {
        return logs.stream().filter(log -> log.getStatus() == status).count();
    }

    private long countChannel(List<NotificationLog> logs, NotificationChannel channel) {
        return logs.stream().filter(log -> log.getChannel() == channel).count();
    }
}
