package org.streamhub.api.v1.analytics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.analytics.entity.AnalyticsEvent;
import org.streamhub.api.v1.analytics.entity.ContentKind;
import org.streamhub.api.v1.analytics.entity.DeviceKind;
import org.streamhub.api.v1.analytics.entity.EventType;
import org.streamhub.api.v1.analytics.repository.AnalyticsEventRepository;

/**
 * Seeds the web-analytics demo dataset (콘텐츠 분석). Idempotent (skips when the event table already
 * holds rows). The fixed-seed {@link Random} makes the dataset <em>shape</em> reproducible — most
 * importantly a deliberate popularity skew (a Zipf-like weighting over content targets) so the
 * dashboard clearly separates winners from losers. Absolute dates are anchored to
 * {@link LocalDateTime#now()} and biased toward recent days (slight upward trend), so the
 * {@value #WINDOW_DAYS}-day window rolls forward to stay current. All values are demo/fictional.
 */
@Slf4j
@Component
@Order(21)
public class AnalyticsSeeder implements CommandLineRunner {

    private static final long SEED = 921L;
    private static final int TARGET_EVENTS = 2000;
    private static final int WINDOW_DAYS = 30;
    private static final int SESSION_POOL = 400;

    private static final int VIDEO_COUNT = 15;
    private static final int ALBUM_COUNT = 12;
    private static final int POST_COUNT = 10;

    /** Plain page-view routes, weighted toward high-traffic ones (duplicates raise the odds). */
    private static final String[] PAGE_PATHS = {
            "/", "/", "/", "/albums", "/albums", "/churches", "/video", "/community"
    };

    private static final String[] REFERRERS = {
            "google", "google", "naver", "naver", "instagram", "direct", "direct", "kakaotalk"
    };

    private final AnalyticsEventRepository analyticsEventRepository;

    public AnalyticsSeeder(AnalyticsEventRepository analyticsEventRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
    }

    @Override
    public void run(String... args) {
        if (analyticsEventRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);
        String[] sessionPool = sessionPool(rnd);

        List<AnalyticsEvent> events = new ArrayList<>(TARGET_EVENTS);
        for (int i = 0; i < TARGET_EVENTS; i++) {
            LocalDateTime occurredAt = distributedDateTime(now, rnd);
            String sessionId = sessionPool[rnd.nextInt(sessionPool.length)];
            Long memberId = rnd.nextInt(100) < 60 ? null : (long) (1 + rnd.nextInt(40));
            DeviceKind device = pickDevice(rnd);
            String referrer = REFERRERS[rnd.nextInt(REFERRERS.length)];

            events.add(buildEvent(rnd, occurredAt, sessionId, memberId, device, referrer));
        }
        analyticsEventRepository.saveAll(events);
        log.info("Seeded {} analytics events over the last {} days", TARGET_EVENTS, WINDOW_DAYS);
    }

    /**
     * Builds one event by the mix CONTENT_VIEW ~55% / PAGE_VIEW ~35% / SESSION_START ~10%. Content
     * views carry a skewed target (popular vs underperforming), a title, a path and a long dwell.
     */
    private AnalyticsEvent buildEvent(Random rnd, LocalDateTime occurredAt, String sessionId,
                                      Long memberId, DeviceKind device, String referrer) {
        int roll = rnd.nextInt(100);
        AnalyticsEvent.AnalyticsEventBuilder builder = AnalyticsEvent.builder()
                .sessionId(sessionId)
                .memberId(memberId)
                .deviceType(device)
                .referrer(referrer)
                .occurredAt(occurredAt)
                .createdAt(occurredAt);

        if (roll < 55) {
            ContentKind kind = pickContentKind(rnd);
            long targetId = skewedTarget(rnd, contentCount(kind));
            return builder
                    .type(EventType.CONTENT_VIEW)
                    .contentType(kind)
                    .targetId(targetId)
                    .title(titleFor(kind, targetId))
                    .path(pathFor(kind, targetId))
                    .dwellMs(3_000L + (long) (rnd.nextDouble() * 897_000L))
                    .build();
        }
        if (roll < 90) {
            return builder
                    .type(EventType.PAGE_VIEW)
                    .contentType(ContentKind.PAGE)
                    .path(PAGE_PATHS[rnd.nextInt(PAGE_PATHS.length)])
                    .dwellMs(1_000L + (long) (rnd.nextDouble() * 59_000L))
                    .build();
        }
        return builder
                .type(EventType.SESSION_START)
                .contentType(ContentKind.PAGE)
                .path("/")
                .build();
    }

    /**
     * Zipf-like skew over {@code [1..count]}: low ids (rank 1, 2, ...) win far more views than high
     * ids. {@code u^2} concentrates draws near 0, mapped to rank → a few clear winners and a long
     * tail of underperforming content.
     */
    private long skewedTarget(Random rnd, int count) {
        double u = rnd.nextDouble();
        int rank = (int) Math.floor(u * u * count);
        return 1L + Math.min(rank, count - 1);
    }

    private ContentKind pickContentKind(Random rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 50) {
            return ContentKind.VIDEO;
        }
        if (roll < 80) {
            return ContentKind.ALBUM;
        }
        return ContentKind.POST;
    }

    private int contentCount(ContentKind kind) {
        return switch (kind) {
            case VIDEO -> VIDEO_COUNT;
            case ALBUM -> ALBUM_COUNT;
            case POST -> POST_COUNT;
            case PAGE -> 1;
        };
    }

    private String titleFor(ContentKind kind, long targetId) {
        return switch (kind) {
            case VIDEO -> "주일 예배 실황 #" + targetId;
            case ALBUM -> "새벽 찬양 Vol." + targetId;
            case POST -> "이번 주 소식 #" + targetId;
            case PAGE -> "페이지";
        };
    }

    private String pathFor(ContentKind kind, long targetId) {
        return switch (kind) {
            case VIDEO -> "/video/" + targetId;
            case ALBUM -> "/albums/" + targetId;
            case POST -> "/community/" + targetId;
            case PAGE -> "/";
        };
    }

    /** Device mix ~ PC 45 / MOBILE 45 / TABLET 10. */
    private DeviceKind pickDevice(Random rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 45) {
            return DeviceKind.PC;
        }
        if (roll < 90) {
            return DeviceKind.MOBILE;
        }
        return DeviceKind.TABLET;
    }

    /** A reusable pool of {@value #SESSION_POOL} session ids (so distinct sessions ≈ 400). */
    private String[] sessionPool(Random rnd) {
        String[] pool = new String[SESSION_POOL];
        for (int i = 0; i < SESSION_POOL; i++) {
            pool[i] = "sess-" + Long.toHexString(rnd.nextLong());
        }
        return pool;
    }

    /**
     * Produces a {@link LocalDateTime} within the last {@link #WINDOW_DAYS} days, biased toward
     * recent dates ({@code 1 - sqrt(u)} weighting → slight uptrend), at a plausible daytime hour.
     */
    private LocalDateTime distributedDateTime(LocalDateTime now, Random rnd) {
        int daysAgo = (int) Math.round((1.0 - Math.sqrt(rnd.nextDouble())) * (WINDOW_DAYS - 1));
        LocalDateTime when = now.minusDays(daysAgo)
                .withHour(8 + rnd.nextInt(15))
                .withMinute(rnd.nextInt(60))
                .withSecond(rnd.nextInt(60))
                .withNano(0);
        return when.isAfter(now)
                ? now.minusHours(1 + rnd.nextInt(6)).withSecond(0).withNano(0)
                : when;
    }
}
