package org.streamhub.api.v1.logarchive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires the weekly log archive-then-purge job. A separate bean from {@link LogArchiveService} so the
 * call goes through the Spring proxy (the downstream purge needs its transaction). Requires
 * {@code @EnableScheduling} (already set on {@code StreamhubApiApplication}).
 *
 * <p>Cron defaults to Sundays at 04:00; override with {@code app.log.archive-cron}.
 */
@Slf4j
@Component
public class LogArchiveScheduler {

    private final LogArchiveService logArchiveService;

    public LogArchiveScheduler(LogArchiveService logArchiveService) {
        this.logArchiveService = logArchiveService;
    }

    @Scheduled(cron = "${app.log.archive-cron:0 0 4 * * SUN}")
    public void run() {
        try {
            LogArchiveService.ArchiveResult result = logArchiveService.archiveAndPurge();
            log.info("Weekly log archive complete: {} action-log, {} security-event row(s).",
                    result.actionLogs(), result.securityEvents());
        } catch (RuntimeException e) {
            log.warn("Weekly log archive run failed: {}", e.getMessage());
        }
    }
}
