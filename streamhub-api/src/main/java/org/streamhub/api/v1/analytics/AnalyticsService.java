package org.streamhub.api.v1.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.analytics.dto.AnalyticsBreakdownDto;
import org.streamhub.api.v1.analytics.dto.AnalyticsOverviewDto;
import org.streamhub.api.v1.analytics.dto.ContentStatDto;
import org.streamhub.api.v1.analytics.dto.CountItemDto;
import org.streamhub.api.v1.analytics.dto.EventIngestRequest;
import org.streamhub.api.v1.analytics.dto.TimeseriesPointDto;
import org.streamhub.api.v1.analytics.entity.AnalyticsEvent;
import org.streamhub.api.v1.analytics.entity.ContentKind;
import org.streamhub.api.v1.analytics.entity.DeviceKind;
import org.streamhub.api.v1.analytics.entity.EventType;
import org.streamhub.api.v1.analytics.repository.AnalyticsEventRepository;

/**
 * Web-analytics pipeline (Firebase-style). The public side persists one cheap row per ingested
 * event, parsing the client-supplied enums defensively so malformed browser input never 500s. The
 * admin side computes every aggregate in memory from a single scan over the demo dataset — no
 * grouped SQL, window functions or analytics store needed.
 */
@Service
public class AnalyticsService {

    /** Look-back window for the timeseries trend. */
    private static final int TIMESERIES_DAYS = 30;

    /** Number of top referrers returned in the breakdown. */
    private static final int TOP_REFERRERS = 6;

    /** Number of top paths returned in the breakdown. */
    private static final int TOP_PATHS = 8;

    private final AnalyticsEventRepository analyticsEventRepository;

    public AnalyticsService(AnalyticsEventRepository analyticsEventRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
    }

    /** Persists one ingested event, stamping {@code occurredAt=now()} and defaulting bad enums. */
    @Transactional
    public void ingest(EventIngestRequest request) {
        if (request == null) {
            return;
        }
        analyticsEventRepository.save(AnalyticsEvent.builder()
                .type(parseType(request.type()))
                .contentType(parseContentKind(request.contentType()))
                .targetId(request.targetId())
                .title(clamp(request.title(), 200))
                .path(clamp(request.path(), 200))
                .sessionId(clamp(request.sessionId(), 64))
                .memberId(request.memberId())
                .deviceType(parseDevice(request.deviceType()))
                .referrer(clamp(request.referrer(), 300))
                .dwellMs(request.dwellMs())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    /** Persists a batch of ingested events; lenient, skips nulls. */
    @Transactional
    public void ingestBatch(List<EventIngestRequest> requests) {
        if (requests == null) {
            return;
        }
        for (EventIngestRequest request : requests) {
            ingest(request);
        }
    }

    /** All-time overview: totals, distinct sessions/visitors, view split and average dwell. */
    @Transactional(readOnly = true)
    public AnalyticsOverviewDto overview() {
        List<AnalyticsEvent> events = analyticsEventRepository.findAll();

        long totalSessions = events.stream()
                .map(AnalyticsEvent::getSessionId)
                .filter(id -> id != null)
                .distinct()
                .count();
        long uniqueVisitors = events.stream()
                .map(AnalyticsEvent::getMemberId)
                .filter(id -> id != null)
                .distinct()
                .count();
        long pageViews = events.stream().filter(e -> e.getType() == EventType.PAGE_VIEW).count();
        long contentViews = events.stream().filter(e -> e.getType() == EventType.CONTENT_VIEW).count();
        long avgDwellMs = Math.round(events.stream()
                .map(AnalyticsEvent::getDwellMs)
                .filter(d -> d != null)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));

        return new AnalyticsOverviewDto(
                events.size(), totalSessions, uniqueVisitors, pageViews, contentViews, avgDwellMs);
    }

    /**
     * Per-content view stats, grouped by {@code (contentType, targetId)} over CONTENT_VIEW events
     * with a target, sorted by views descending. The frontend slices top (popular) and bottom
     * (underperforming) off this single list.
     */
    @Transactional(readOnly = true)
    public List<ContentStatDto> contentPerformance() {
        Map<String, List<AnalyticsEvent>> grouped = analyticsEventRepository.findAll().stream()
                .filter(e -> e.getType() == EventType.CONTENT_VIEW && e.getTargetId() != null)
                .collect(Collectors.groupingBy(e -> e.getContentType() + "#" + e.getTargetId()));

        return grouped.values().stream()
                .map(this::toContentStat)
                .sorted(Comparator.comparingLong(ContentStatDto::views).reversed())
                .toList();
    }

    /**
     * Daily event and distinct-session counts across the last {@value #TIMESERIES_DAYS} days
     * (oldest first), zero-filled so every day in the window is present.
     */
    @Transactional(readOnly = true)
    public List<TimeseriesPointDto> timeseries() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(TIMESERIES_DAYS - 1L);
        List<AnalyticsEvent> events =
                analyticsEventRepository.findByOccurredAtBetween(from.atStartOfDay(), endOfDay(to));

        Map<LocalDate, List<AnalyticsEvent>> byDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getOccurredAt().toLocalDate()));

        Map<LocalDate, TimeseriesPointDto> filled = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<AnalyticsEvent> dayEvents = byDay.getOrDefault(day, List.of());
            long sessions = dayEvents.stream()
                    .map(AnalyticsEvent::getSessionId)
                    .filter(id -> id != null)
                    .distinct()
                    .count();
            filled.put(day, new TimeseriesPointDto(day, dayEvents.size(), sessions));
        }
        return List.copyOf(filled.values());
    }

    /** Categorical breakdown: device mix plus top referrers and paths. */
    @Transactional(readOnly = true)
    public AnalyticsBreakdownDto breakdown() {
        List<AnalyticsEvent> events = analyticsEventRepository.findAll();

        Map<DeviceKind, Long> byDevice = new EnumMap<>(DeviceKind.class);
        for (DeviceKind kind : DeviceKind.values()) {
            byDevice.put(kind, 0L);
        }
        for (AnalyticsEvent event : events) {
            if (event.getDeviceType() != null) {
                byDevice.merge(event.getDeviceType(), 1L, Long::sum);
            }
        }

        List<CountItemDto> topReferrers = topCounts(
                events, AnalyticsEvent::getReferrer, TOP_REFERRERS);
        List<CountItemDto> topPaths = topCounts(events, AnalyticsEvent::getPath, TOP_PATHS);

        return new AnalyticsBreakdownDto(byDevice, topReferrers, topPaths);
    }

    // --- helpers -----------------------------------------------------------

    private ContentStatDto toContentStat(List<AnalyticsEvent> group) {
        AnalyticsEvent any = group.get(0);
        long avgDwellMs = Math.round(group.stream()
                .map(AnalyticsEvent::getDwellMs)
                .filter(d -> d != null)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));
        LocalDateTime lastViewedAt = group.stream()
                .map(AnalyticsEvent::getOccurredAt)
                .max(Comparator.naturalOrder())
                .orElse(any.getOccurredAt());
        return new ContentStatDto(any.getContentType(), any.getTargetId(), any.getTitle(),
                group.size(), avgDwellMs, lastViewedAt);
    }

    private List<CountItemDto> topCounts(List<AnalyticsEvent> events,
                                         java.util.function.Function<AnalyticsEvent, String> field,
                                         int limit) {
        return events.stream()
                .map(field)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new CountItemDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Inclusive end-of-day bound for a date (23:59:59.999999999). */
    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private EventType parseType(String raw) {
        if (raw == null) {
            return EventType.PAGE_VIEW;
        }
        try {
            return EventType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EventType.PAGE_VIEW;
        }
    }

    private ContentKind parseContentKind(String raw) {
        if (raw == null) {
            return ContentKind.PAGE;
        }
        try {
            return ContentKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ContentKind.PAGE;
        }
    }

    private DeviceKind parseDevice(String raw) {
        if (raw == null) {
            return DeviceKind.PC;
        }
        try {
            return DeviceKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DeviceKind.PC;
        }
    }

    private String clamp(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
