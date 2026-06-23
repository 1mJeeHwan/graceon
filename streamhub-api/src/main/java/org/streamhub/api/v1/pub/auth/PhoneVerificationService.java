package org.streamhub.api.v1.pub.auth;

import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.pub.auth.dto.PhoneVerifyResponse;

/**
 * Mock phone identity-verification (휴대폰 본인인증) backed by Redis. The code generation,
 * expiry, attempt cap and one-time consume are real; only the delivery is mocked — there is no
 * SMS gateway in this portfolio environment, so in demo mode the code is returned in the response
 * ({@code app.verification.expose-code=true}). Wire a real SMS provider and set the flag false to
 * stop exposing it.
 */
@Service
public class PhoneVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 5;

    private static final String CODE_KEY = "signup:code:";
    private static final String ATTEMPTS_KEY = "signup:attempts:";
    private static final String COOLDOWN_KEY = "signup:cooldown:";
    private static final String VERIFIED_KEY = "signup:verified:";

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();
    private final boolean exposeCode;

    public PhoneVerificationService(
            StringRedisTemplate redis,
            @Value("${app.verification.expose-code:true}") boolean exposeCode) {
        this.redis = redis;
        this.exposeCode = exposeCode;
    }

    /** Generates and "sends" a 6-digit code for {@code phone}, rate-limited by a short cooldown. */
    public PhoneVerifyResponse requestCode(String phone) {
        String key = normalize(phone);
        if (Boolean.TRUE.equals(redis.hasKey(COOLDOWN_KEY + key))) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "잠시 후 다시 시도해 주세요");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set(CODE_KEY + key, code, CODE_TTL);
        redis.delete(ATTEMPTS_KEY + key);
        redis.opsForValue().set(COOLDOWN_KEY + key, "1", COOLDOWN_TTL);
        return new PhoneVerifyResponse(CODE_TTL.toSeconds(), exposeCode ? code : null);
    }

    /** Confirms {@code code} for {@code phone}; on success marks the phone verified (one-time). */
    public void confirmCode(String phone, String code) {
        String key = normalize(phone);
        Long attempts = redis.opsForValue().increment(ATTEMPTS_KEY + key);
        if (attempts != null && attempts == 1L) {
            redis.expire(ATTEMPTS_KEY + key, CODE_TTL);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redis.delete(CODE_KEY + key);
            throw new ApiException(ResultCode.INVALID_PARAMETER, "인증 시도 횟수를 초과했습니다. 다시 요청해 주세요");
        }
        String stored = redis.opsForValue().get(CODE_KEY + key);
        if (stored == null) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "인증번호가 만료되었습니다. 다시 요청해 주세요");
        }
        if (!stored.equals(code)) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "인증번호가 일치하지 않습니다");
        }
        redis.delete(CODE_KEY + key);
        redis.delete(ATTEMPTS_KEY + key);
        redis.opsForValue().set(VERIFIED_KEY + key, "1", VERIFIED_TTL);
    }

    /**
     * Consumes the one-time verified flag for {@code phone}; throws if the phone was not verified
     * (or the flag expired). Called by sign-up so an account can only be created for a phone that
     * just passed verification.
     */
    public void consumeVerified(String phone) {
        String key = normalize(phone);
        if (!Boolean.TRUE.equals(redis.delete(VERIFIED_KEY + key))) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "휴대폰 본인인증을 먼저 완료해 주세요");
        }
    }

    /** Digits-only key so "010-1234-5678" and "01012345678" share one verification slot. */
    private String normalize(String phone) {
        return phone.replaceAll("\\D", "");
    }
}
