package org.streamhub.api.v1.visit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.visit.entity.DeviceType;
import org.streamhub.api.v1.visit.entity.VisitLog;
import org.streamhub.api.v1.visit.repository.VisitLogRepository;

/**
 * Seeds the front-site visit-statistics demo dataset (접속 통계). Idempotent (skips when the
 * visit-log table already holds rows). The fixed-seed {@link Random} makes the dataset
 * <em>shape</em> (browser/OS/device mix, path weighting, member ratio) reproducible across runs;
 * the absolute dates are <em>not</em> fixed — every row is anchored to {@link LocalDateTime#now()},
 * so the {@value #WINDOW_DAYS}-day window rolls forward to stay current relative to today, biased
 * toward recent days (slight upward trend). All values are demo/fictional and the IP is masked.
 */
@Slf4j
@Component
@Order(17)
public class VisitSeeder implements CommandLineRunner {

    private static final long SEED = 917L;
    private static final int TARGET_VISITS = 400;
    private static final int WINDOW_DAYS = 30;

    /** Front-site paths, weighted toward the high-traffic ones (duplicates raise the odds). */
    private static final String[] PATHS = {
            "/", "/", "/", "/",
            "/albums", "/albums", "/albums",
            "/churches", "/churches",
            "/content", "/content",
            "/donation", "/community", "/notice", "/search"
    };

    /**
     * Realistic user-agent presets. Each carries its derived browser / OS / device class so the
     * stored parsed fields stay consistent with the raw UA string.
     */
    private static final UaPreset[] UA_PRESETS = {
            new UaPreset(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36",
                    "Chrome", "Windows", DeviceType.PC),
            new UaPreset(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36",
                    "Chrome", "macOS", DeviceType.PC),
            new UaPreset(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) "
                            + "Version/17.4 Safari/605.1.15",
                    "Safari", "macOS", DeviceType.PC),
            new UaPreset(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
                    "Edge", "Windows", DeviceType.PC),
            new UaPreset(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0",
                    "Firefox", "Windows", DeviceType.PC),
            new UaPreset(
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 "
                            + "(KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
                    "Safari", "iOS", DeviceType.MOBILE),
            new UaPreset(
                    "Mozilla/5.0 (Linux; Android 14; SM-S921N) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Mobile Safari/537.36",
                    "Chrome", "Android", DeviceType.MOBILE),
            new UaPreset(
                    "Mozilla/5.0 (Linux; Android 14; SM-S921N) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "SamsungBrowser/24.0 Chrome/117.0.0.0 Mobile Safari/537.36",
                    "Samsung Internet", "Android", DeviceType.MOBILE),
            new UaPreset(
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Mobile Safari/537.36",
                    "Chrome", "Android", DeviceType.MOBILE),
            new UaPreset(
                    "Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15 "
                            + "(KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
                    "Safari", "iOS", DeviceType.TABLET),
            new UaPreset(
                    "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36",
                    "Chrome", "Android", DeviceType.TABLET)
    };

    private final VisitLogRepository visitLogRepository;

    public VisitSeeder(VisitLogRepository visitLogRepository) {
        this.visitLogRepository = visitLogRepository;
    }

    @Override
    public void run(String... args) {
        if (visitLogRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);

        List<VisitLog> logs = new ArrayList<>(TARGET_VISITS);
        for (int i = 0; i < TARGET_VISITS; i++) {
            UaPreset ua = UA_PRESETS[rnd.nextInt(UA_PRESETS.length)];
            LocalDateTime visitedAt = distributedDateTime(now, rnd);
            Long memberId = rnd.nextInt(100) < 60 ? null : (long) (1 + rnd.nextInt(40));

            logs.add(VisitLog.builder()
                    .visitedAt(visitedAt)
                    .ipMasked(maskedIp(rnd))
                    .userAgent(ua.userAgent())
                    .browser(ua.browser())
                    .os(ua.os())
                    .deviceType(ua.deviceType())
                    .path(PATHS[rnd.nextInt(PATHS.length)])
                    .memberId(memberId)
                    .createdAt(visitedAt)
                    .build());
        }
        visitLogRepository.saveAll(logs);
        log.info("Seeded {} visit logs over the last {} days", TARGET_VISITS, WINDOW_DAYS);
    }

    /** Random masked IP like {@code "<1-223>.<0-255>.*.*"} (first two octets only). */
    private String maskedIp(Random rnd) {
        return (1 + rnd.nextInt(223)) + "." + rnd.nextInt(256) + ".*.*";
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

    /** A user-agent string paired with its derived browser / OS / device class. */
    private record UaPreset(String userAgent, String browser, String os, DeviceType deviceType) {
    }
}
