package org.streamhub.api.v1.pub.auth;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Tracks which phone numbers have just passed Iamport(포트원) identity-verification, as a short-lived
 * one-time flag in Redis. The certification step sets the flag; sign-up consumes it so an account can
 * only be created for a phone that was verified moments earlier (and only once).
 */
@Service
public class PhoneVerificationService {

    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final String VERIFIED_KEY = "signup:verified:";

    private final StringRedisTemplate redis;

    public PhoneVerificationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Marks {@code phone} as verified for the next {@value #VERIFIED_TTL} (one-time, consumed by sign-up). */
    public void markVerified(String phone) {
        redis.opsForValue().set(VERIFIED_KEY + normalize(phone), "1", VERIFIED_TTL);
    }

    /**
     * Consumes the one-time verified flag for {@code phone}; throws if the phone was not verified
     * (or the flag expired).
     */
    public void consumeVerified(String phone) {
        if (!Boolean.TRUE.equals(redis.delete(VERIFIED_KEY + normalize(phone)))) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "휴대폰 본인인증을 먼저 완료해 주세요");
        }
    }

    /** Digits-only key so "010-1234-5678" and "01012345678" share one verification slot. */
    private String normalize(String phone) {
        return phone.replaceAll("\\D", "");
    }
}
