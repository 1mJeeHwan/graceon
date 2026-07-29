package org.streamhub.api.base.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Revocation list for member tokens, keyed by {@code jti}.
 *
 * <p>Member sessions are a single stateless token with no refresh and no server-side record, so
 * before this existed "log out" meant deleting the copy in the browser and nothing else: a token
 * lifted by an XSS stayed valid until it expired, and since members cannot change their password
 * either, the account was simply gone for that whole window. A denylist is the smallest thing that
 * makes logout mean something.
 *
 * <p>Entries expire when the token would have expired anyway, so the keyspace is bounded by the
 * number of logouts inside one token lifetime rather than growing forever.
 *
 * <p>Fails <b>open</b> when Redis is unreachable: a lookup error must not lock every member out of
 * the public site. That is the same trade the login lockout counter makes, and it is acceptable
 * because the denylist narrows an already-short window rather than being the primary access control.
 */
@Slf4j
@Component
public class MemberTokenDenylist {

    private static final String KEY_PREFIX = "memberTokenDenied:";

    private final StringRedisTemplate redisTemplate;

    public MemberTokenDenylist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Revokes a verified token for whatever remains of its lifetime. No-op if it has no jti. */
    public void revoke(DecodedJWT jwt) {
        String jti = jwt.getId();
        if (jti == null || jti.isBlank()) {
            return;
        }
        Duration remaining = remainingLifetime(jwt);
        if (remaining.isZero() || remaining.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", remaining);
        } catch (RuntimeException e) {
            log.warn("Could not revoke member token {}: {}", jti, e.getMessage());
        }
    }

    /** Whether this token has been revoked. Answers false when the store cannot be reached. */
    public boolean isRevoked(DecodedJWT jwt) {
        String jti = jwt.getId();
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (RuntimeException e) {
            log.warn("Denylist lookup failed for {}, allowing the request: {}", jti, e.getMessage());
            return false;
        }
    }

    private Duration remainingLifetime(DecodedJWT jwt) {
        Instant expiresAt = jwt.getExpiresAtAsInstant();
        return expiresAt == null ? Duration.ZERO : Duration.between(Instant.now(), expiresAt);
    }
}
