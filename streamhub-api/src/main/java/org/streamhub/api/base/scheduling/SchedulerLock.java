package org.streamhub.api.base.scheduling;

import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis lease that lets only one instance run a given scheduled job at a time.
 *
 * <p>Every {@code @Scheduled} job in this service used to assume a single instance without saying
 * so anywhere. Scaling to two would have had both charging subscriptions, expiring the same points,
 * and draining the same outbox rows in the same minute. Where a job is genuinely idempotent that is
 * merely wasteful, but point expiry writes a ledger row per member per run — a duplicate there is a
 * double deduction, and nothing in the schema forbids it.
 *
 * <p>A lease, not a lock: {@code SET NX PX} takes it and it expires on its own, so an instance that
 * dies mid-job cannot wedge the schedule forever. The trade is the classic one — if a job outlives
 * its lease another instance may start a second copy, so callers pass a TTL comfortably longer than
 * the job's worst observed runtime, and jobs stay idempotent regardless. This is deliberately not
 * ShedLock: one Redis key and twenty lines beat a dependency plus its schema for five jobs.
 */
@Slf4j
@Component
public class SchedulerLock {

    private static final String KEY_PREFIX = "schedulerLock:";

    /** Identifies this JVM, so a holder only ever releases its own lease. */
    private final String instanceId = UUID.randomUUID().toString();

    private final StringRedisTemplate redisTemplate;

    public SchedulerLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Runs {@code job} if this instance wins the lease for {@code name}, otherwise returns quietly.
     *
     * <p>Redis being unreachable fails <b>closed</b> — the job is skipped, not run unguarded. These
     * are periodic jobs that lose nothing by waiting for the next tick, whereas running them
     * unguarded during an outage is exactly the double-charge this class exists to prevent.
     *
     * @param name job identifier, unique per schedule
     * @param ttl  lease lifetime; longer than the job's worst-case runtime
     * @param job  the work to run while holding the lease
     */
    public void runIfLeader(String name, Duration ttl, Runnable job) {
        String key = KEY_PREFIX + name;
        Boolean acquired;
        try {
            acquired = redisTemplate.opsForValue().setIfAbsent(key, instanceId, ttl);
        } catch (RuntimeException e) {
            log.warn("Skipping scheduled job '{}': lock store unavailable ({})", name, e.getMessage());
            return;
        }
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Skipping scheduled job '{}': another instance holds the lease", name);
            return;
        }
        try {
            job.run();
        } finally {
            release(key);
        }
    }

    /**
     * Releases the lease, but only if this instance still owns it. Without the ownership check a
     * job that overran its TTL would delete the lease a *different* instance had since taken,
     * letting a third start immediately.
     */
    private void release(String key) {
        try {
            if (instanceId.equals(redisTemplate.opsForValue().get(key))) {
                redisTemplate.delete(key);
            }
        } catch (RuntimeException e) {
            // Harmless: the lease expires on its own.
            log.debug("Could not release scheduler lease {}: {}", key, e.getMessage());
        }
    }
}
