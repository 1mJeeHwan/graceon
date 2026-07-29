package org.streamhub.api.v1.member;

import org.streamhub.api.base.scheduling.SchedulerLock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the point-expiry batch on a daily schedule. Requires {@code @EnableScheduling}
 * (set on {@code StreamhubApiApplication}); the recovery logic lives in {@link PointService}.
 */
@Component
public class PointExpiryScheduler {

    private final SchedulerLock schedulerLock;

    private final PointService pointService;

    public PointExpiryScheduler(PointService pointService,
            SchedulerLock schedulerLock) {
        this.schedulerLock = schedulerLock;
        this.pointService = pointService;
    }

    /** Runs daily at 04:00; recovers due accruals and records the expiry ledger rows. */
    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        // The most damaging job to double-run: expiry writes a ledger row per member and nothing
        // in the schema makes a second deduction a constraint violation.
        schedulerLock.runIfLeader("pointExpiry", Duration.ofMinutes(30), pointService::expirePoints);
    }
}
