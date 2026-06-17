package org.streamhub.api.v1.member;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the point-expiry batch on a daily schedule. Requires {@code @EnableScheduling}
 * (set on {@code StreamhubApiApplication}); the recovery logic lives in {@link PointService}.
 */
@Component
public class PointExpiryScheduler {

    private final PointService pointService;

    public PointExpiryScheduler(PointService pointService) {
        this.pointService = pointService;
    }

    /** Runs daily at 04:00; recovers due accruals and records the expiry ledger rows. */
    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        pointService.expirePoints();
    }
}
