package org.streamhub.api.v1.visit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.visit.dto.DailyCountDto;
import org.streamhub.api.v1.visit.dto.PathCountDto;
import org.streamhub.api.v1.visit.dto.VisitLogDto;
import org.streamhub.api.v1.visit.dto.VisitSearchRequest;
import org.streamhub.api.v1.visit.dto.VisitSummaryDto;
import org.streamhub.api.v1.visit.entity.DeviceType;
import org.streamhub.api.v1.visit.entity.VisitLog;
import org.streamhub.api.v1.visit.repository.VisitLogRepository;

/**
 * Front-site visit statistics (접속 통계).
 *
 * <p>Every aggregate is a grouped query. An earlier revision loaded the whole table with
 * {@code findAll()} and counted in a Java stream, which was fine at the ~400 demo rows it was
 * written for and unbounded everywhere else: VISIT_LOG is append-only, so its size tracks total
 * site traffic forever, and the read-only transaction kept every loaded row in the persistence
 * context on top of the list itself. The dashboard would have died of the site succeeding.
 *
 * <p>Zero-filling stays in Java on purpose — the database returns only days and device types that
 * actually saw traffic, and generating the missing ones in SQL costs more than it saves for a
 * window of at most a few hundred entries.
 */
@Slf4j
@Service
public class VisitService {

    /** Default look-back window when the request supplies no bounds. */
    private static final int DEFAULT_WINDOW_DAYS = 30;

    /** Cap on rows returned by the list endpoint. */
    private static final int LIST_LIMIT = 500;

    /** Number of top paths returned in the summary. */
    private static final int TOP_PATHS = 5;

    private final VisitLogRepository visitLogRepository;

    public VisitService(VisitLogRepository visitLogRepository) {
        this.visitLogRepository = visitLogRepository;
    }

    /**
     * Records one real site visit (접속 통계), keyed by the client's IP masked to its first two
     * octets ({@code "211.45.*.*"}). Called from the public analytics ingest on every PAGE_VIEW, so
     * the visit stat reflects who actually accesses the site — not just the seeded demo rows.
     * Best-effort: a failure here never breaks the page-view ingest.
     * ponytail: one row per page view; add a per-session+path cooldown if volume ever matters.
     */
    @Transactional
    public void record(String clientIp, String userAgent, String path, String deviceType, Long memberId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            visitLogRepository.save(VisitLog.builder()
                    .visitedAt(now)
                    .createdAt(now)
                    .ipMasked(maskIp(clientIp))
                    .userAgent(clamp(userAgent, 300))
                    .deviceType(deviceFrom(deviceType, userAgent))
                    .path(clamp(path, 200))
                    .memberId(memberId)
                    .build());
        } catch (RuntimeException ex) {
            log.warn("visit record skipped: {}", ex.getMessage());
        }
    }

    /** Masks an IP to its first two octets (IPv4) or first two hextets (IPv6); null/blank → null. */
    static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String s = ip.trim();
        if (s.indexOf('.') > 0) {
            String[] p = s.split("\\.");
            return p.length == 4 ? p[0] + "." + p[1] + ".*.*" : s;
        }
        if (s.contains(":")) {
            String[] p = s.split(":");
            return p.length >= 2 ? p[0] + ":" + p[1] + ":*" : s;
        }
        return s;
    }

    private DeviceType deviceFrom(String deviceType, String userAgent) {
        String dt = deviceType == null ? "" : deviceType.toUpperCase(java.util.Locale.ROOT);
        if (dt.contains("MOBILE")) {
            return DeviceType.MOBILE;
        }
        if (dt.contains("PC") || dt.contains("DESKTOP")) {
            return DeviceType.PC;
        }
        return userAgent != null && userAgent.toLowerCase(java.util.Locale.ROOT).contains("mobi")
                ? DeviceType.MOBILE : DeviceType.PC;
    }

    private String clamp(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** Visit list within the requested period (newest first), capped to {@value #LIST_LIMIT} rows. */
    @Transactional(readOnly = true)
    public List<VisitLogDto> list(VisitSearchRequest request) {
        LocalDate to = request != null && request.toDate() != null ? request.toDate() : LocalDate.now();
        LocalDate from = request != null && request.fromDate() != null
                ? request.fromDate() : to.minusDays(DEFAULT_WINDOW_DAYS);
        DeviceType deviceType = request != null ? request.deviceType() : null;
        String keyword = request != null && request.keyword() != null
                ? request.keyword().trim().toLowerCase() : null;

        // Sorting and the row cap are pushed to the database; the two optional filters stay in Java
        // because they are cheap over an already-capped page and keep the query a single derivation.
        // If they ever need to filter *before* the cap, they belong in the query too.
        return visitLogRepository
                .findByVisitedAtBetweenOrderByVisitedAtDesc(
                        from.atStartOfDay(), endOfDay(to), PageRequest.of(0, LIST_LIMIT))
                .stream()
                .filter(log -> deviceType == null || deviceType == log.getDeviceType())
                .filter(log -> matchesKeyword(log, keyword))
                .map(VisitLogDto::from)
                .toList();
    }

    /**
     * Daily visit counts across the requested range (oldest first), with zero-fill so every day in
     * the window is present. Falls back to the last {@value #DEFAULT_WINDOW_DAYS} days when no range
     * is given.
     */
    @Transactional(readOnly = true)
    public List<DailyCountDto> daily(VisitSearchRequest request) {
        LocalDate to = request != null && request.toDate() != null ? request.toDate() : LocalDate.now();
        LocalDate from = request != null && request.fromDate() != null
                ? request.fromDate() : to.minusDays(DEFAULT_WINDOW_DAYS - 1L);

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (Object[] row : visitLogRepository.countPerDay(from.atStartOfDay(), endOfDay(to))) {
            counts.put(toLocalDate(row[0]), (Long) row[1]);
        }

        Map<LocalDate, Long> filled = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            filled.put(day, counts.getOrDefault(day, 0L));
        }
        return filled.entrySet().stream()
                .map(entry -> new DailyCountDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** All-time traffic summary: totals, today, approx unique IPs, top paths and device breakdown. */
    @Transactional(readOnly = true)
    public VisitSummaryDto summary() {
        LocalDate today = LocalDate.now();

        long total = visitLogRepository.count();
        long todayVisits = visitLogRepository
                .countByVisitedAtBetween(today.atStartOfDay(), endOfDay(today));
        long uniqueIpApprox = visitLogRepository.countDistinctIpMasked();

        List<PathCountDto> topPaths = visitLogRepository.countPerPath(PageRequest.of(0, TOP_PATHS))
                .stream()
                .map(row -> new PathCountDto((String) row[0], (Long) row[1]))
                .toList();

        // Every device type appears, including the ones with no traffic, so the chart keeps a
        // stable set of series instead of columns appearing and vanishing between refreshes.
        Map<DeviceType, Long> deviceBreakdown = new EnumMap<>(DeviceType.class);
        for (DeviceType type : DeviceType.values()) {
            deviceBreakdown.put(type, 0L);
        }
        for (Object[] row : visitLogRepository.countPerDeviceType()) {
            deviceBreakdown.put((DeviceType) row[0], (Long) row[1]);
        }

        return new VisitSummaryDto(total, todayVisits, uniqueIpApprox, topPaths, deviceBreakdown);
    }

    /** JPA returns {@code cast(... as date)} as java.sql.Date on some providers, LocalDate on others. */
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    // --- helpers -----------------------------------------------------------

    /** Inclusive end-of-day bound for a date (23:59:59.999999999). */
    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private boolean matchesKeyword(VisitLog log, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(log.getPath(), keyword)
                || containsIgnoreCase(log.getBrowser(), keyword)
                || containsIgnoreCase(log.getOs(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}
