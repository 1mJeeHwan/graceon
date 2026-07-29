package org.streamhub.api.v1.analytics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Per-client (IP) token-bucket throttle for the public, unauthenticated endpoints — analytics
 * ingest, login, signup, chat, worship registration.
 *
 * <p>Each client key gets a bucket of {@value #CAPACITY} tokens refilling at
 * {@value #REFILL_PER_SECOND} tokens/second. A request consumes at least one token; when the bucket
 * is empty the request is denied and the caller drops the event or answers 429 (never a 500 to the
 * browser).
 *
 * <p><b>State lives in Redis.</b> It used to be a process-local {@link ConcurrentHashMap}, which
 * had two problems the moment more than one instance runs: the effective limit multiplied by the
 * instance count, and every redeploy handed all attackers a fresh full bucket. Since these buckets
 * also gate login and signup, that is a security control, not just abuse damping — it belongs in the
 * shared store the lockout counters already use. Redis {@code PEXPIRE} additionally bounds memory,
 * which the old lazy eviction did not: a bucket touched once by a one-shot IP had no way to remove
 * itself and lingered for the process lifetime.
 *
 * <p>Consumption runs as a Lua script so the read-modify-write is atomic on the server; two
 * concurrent requests cannot each observe the pre-decrement token count. If Redis is unreachable the
 * limiter degrades to a process-local bucket rather than failing the request — a throttle that takes
 * the site down when its backing store blips is worse than one that is briefly per-instance.
 */
@Slf4j
@Component
public class PublicIngestRateLimiter {

    /** Maximum burst a client may send before being throttled. */
    static final long CAPACITY = 60L;

    /** Sustained steady-state rate per client (tokens replenished each second). */
    static final double REFILL_PER_SECOND = 5.0;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private static final String KEY_PREFIX = "ingestBucket:";

    /**
     * Idle-bucket lifetime. A bucket refills fully in {@code CAPACITY / REFILL_PER_SECOND} = 12
     * seconds, after which its state is indistinguishable from a fresh one, so expiring at 60s
     * discards nothing an attacker could exploit while keeping the keyspace bounded.
     */
    private static final long KEY_TTL_MILLIS = 60_000L;

    /**
     * Atomic refill-and-consume. Returns 1 when the cost fit, 0 otherwise. Kept all-or-nothing:
     * a partially-charged request would let an oversized batch drain the bucket and still proceed.
     */
    private static final RedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local ts = tonumber(redis.call('HGET', KEYS[1], 'ts'))
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local cost = tonumber(ARGV[4])
            if tokens == nil or ts == nil then
              tokens = capacity
              ts = now
            end
            local elapsed = math.max(0, now - ts) / 1000.0
            tokens = math.min(capacity, tokens + elapsed * refill)
            local allowed = 0
            if tokens >= cost then
              tokens = tokens - cost
              allowed = 1
            end
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            return allowed
            """, Long.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final java.util.function.LongSupplier clock;

    /** Null in unit tests, which exercise the local fallback directly. */
    @Nullable
    private final StringRedisTemplate redisTemplate;

    /**
     * {@code @Autowired} is required, not decorative: the class has a second (test) constructor, and
     * with more than one candidate Spring stops inferring and looks for a no-arg constructor instead.
     */
    @Autowired
    public PublicIngestRateLimiter(StringRedisTemplate redisTemplate) {
        this(System::nanoTime, redisTemplate);
    }

    /** Test seam: local-only limiter driven by a controllable monotonic clock. */
    PublicIngestRateLimiter(java.util.function.LongSupplier clock) {
        this(clock, null);
    }

    private PublicIngestRateLimiter(java.util.function.LongSupplier clock,
                                    @Nullable StringRedisTemplate redisTemplate) {
        this.clock = clock;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to consume a single token for the given client key.
     *
     * @param clientKey stable per-client key, typically the client IP; blank/null keys are treated as
     *                  a shared "unknown" bucket so a missing IP cannot bypass the limit
     * @return {@code true} if the request is allowed, {@code false} if the client is over its limit
     */
    public boolean tryAcquire(String clientKey) {
        return tryAcquire(clientKey, 1L);
    }

    /**
     * Attempts to consume {@code cost} tokens at once, so a request that does proportionally more
     * work (a batch ingest of N events) is charged N tokens instead of one. The cost is clamped to
     * {@code [1, CAPACITY]} — a request always costs something, and one oversized batch can drain
     * but not over-debit the bucket.
     *
     * @param clientKey stable per-client key, typically the client IP
     * @param cost      tokens this request should consume (clamped to {@code [1, CAPACITY]})
     * @return {@code true} if the request is allowed, {@code false} if the client is over its limit
     */
    public boolean tryAcquire(String clientKey, long cost) {
        long tokens = Math.min(CAPACITY, Math.max(1L, cost));
        String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
        Boolean shared = tryAcquireShared(key, tokens);
        return shared != null ? shared : tryAcquireLocal(key, tokens);
    }

    /** Redis-backed consume, or null when Redis is absent/unreachable so the caller can fall back. */
    private Boolean tryAcquireShared(String key, long cost) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Long allowed = redisTemplate.execute(
                    CONSUME_SCRIPT,
                    List.of(KEY_PREFIX + key),
                    String.valueOf(CAPACITY),
                    String.valueOf(REFILL_PER_SECOND),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(cost),
                    String.valueOf(KEY_TTL_MILLIS));
            return allowed != null && allowed == 1L;
        } catch (RuntimeException e) {
            log.warn("Rate-limit store unavailable, falling back to process-local bucket: {}",
                    e.getMessage());
            return null;
        }
    }

    private boolean tryAcquireLocal(String key, long cost) {
        long now = clock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now));
        boolean allowed = bucket.tryConsume(now, cost);
        evictIfReplenished(key, bucket, now);
        return allowed;
    }

    /** Drops a bucket once it has fully refilled, so quiescent IPs do not accumulate forever. */
    private void evictIfReplenished(String key, Bucket bucket, long now) {
        if (bucket.isFull(now)) {
            buckets.remove(key, bucket);
        }
    }

    /** A continuously-refilling token bucket guarded by its own monitor. */
    private static final class Bucket {

        private double tokens;
        private long lastRefillNanos;

        private Bucket(long now) {
            this.tokens = CAPACITY;
            this.lastRefillNanos = now;
        }

        private synchronized boolean tryConsume(long now, long cost) {
            refill(now);
            if (tokens >= cost) {
                tokens -= cost;
                return true;
            }
            return false;
        }

        private synchronized boolean isFull(long now) {
            refill(now);
            return tokens >= CAPACITY;
        }

        private void refill(long now) {
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }
            double refilled = (elapsed / (double) NANOS_PER_SECOND) * REFILL_PER_SECOND;
            tokens = Math.min(CAPACITY, tokens + refilled);
            lastRefillNanos = now;
        }
    }
}
