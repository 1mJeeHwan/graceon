package org.streamhub.api.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.streamhub.api.auth.dto.LoginRequest;
import org.streamhub.api.auth.dto.TokenResponse;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.jwt.JwtTokenProvider;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.util.ClientIpResolver;
import org.streamhub.api.v1.admin.entity.AdminAccount;
import org.streamhub.api.v1.admin.repository.AdminAccountRepository;

/**
 * Authentication: credential check, token issuance, refresh rotation, and logout.
 *
 * <p>Refresh tokens are whitelisted in Redis ({@code refresh:<adminId>}) so logout can
 * invalidate them and rotation can detect reuse of a stale token.
 */
@Service
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    /**
     * Redis key prefix for the login-failure counter. The counter is keyed on
     * {@code adminLoginFail:<ip>:<loginId>} so a brute-force lockout only ever applies to the
     * attacking IP — an abuser cannot lock a victim out of their own account globally by spamming
     * failures from elsewhere (account-DoS surface removed).
     */
    private static final String LOGIN_FAIL_KEY_PREFIX = "adminLoginFail:";

    /** Consecutive failures (within the window) after which login is locked out. */
    private static final int MAX_LOGIN_FAILURES = 5;

    /** How long the failure counter (and therefore the lockout) lives. */
    private static final Duration LOGIN_FAIL_WINDOW = Duration.ofMinutes(10);

    private final AdminAccountRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final org.streamhub.api.v1.actionlog.ActionLogPublisher actionLogPublisher;
    private final org.streamhub.api.v1.security.SecurityMonitor securityMonitor;
    private final ClientIpResolver clientIpResolver;

    public AuthService(
            AdminAccountRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            StringRedisTemplate redisTemplate,
            org.streamhub.api.v1.actionlog.ActionLogPublisher actionLogPublisher,
            org.streamhub.api.v1.security.SecurityMonitor securityMonitor,
            ClientIpResolver clientIpResolver) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.actionLogPublisher = actionLogPublisher;
        this.securityMonitor = securityMonitor;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String account = request.loginId() == null ? "" : request.loginId().trim();
        String failKey = loginFailKey(account);
        if (isLockedOut(failKey)) {
            throw new ApiException(ResultCode.LOGIN_FAILED,
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
        AdminAccount admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> {
                    securityMonitor.recordAuthFailure(request.loginId(), "ADMIN");
                    recordLoginFailure(failKey);
                    return new ApiException(ResultCode.LOGIN_FAILED);
                });
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            securityMonitor.recordAuthFailure(request.loginId(), "ADMIN");
            recordLoginFailure(failKey);
            throw new ApiException(ResultCode.LOGIN_FAILED);
        }
        clearLoginFailures(failKey);
        TokenResponse tokens = issueTokens(admin);
        actionLogPublisher.publishAs(admin.getId(), admin.getName(),
                "LOGIN", "ADMIN", String.valueOf(admin.getId()), "로그인");
        return tokens;
    }

    /**
     * Builds the IP-scoped failure-counter key {@code adminLoginFail:<ip>:<loginId>}. Keying on the
     * client IP (resolved from the current request via {@link ClientIpResolver}, the same way the
     * audit publisher and security monitor read it) confines a lockout to the attacking source, so
     * an abuser cannot lock a victim out of their own account by spamming failures. When no IP can
     * be resolved (off-request, e.g. a test) the key degrades to {@code adminLoginFail:unknown:<id>}.
     */
    private String loginFailKey(String account) {
        String ip = currentIp();
        return LOGIN_FAIL_KEY_PREFIX + (ip == null ? "unknown" : ip) + ":" + account;
    }

    /** Resolves the current request's client IP, or null when off a request thread. */
    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return clientIpResolver.resolve(request);
        }
        return null;
    }

    /**
     * Best-effort lockout check: returns {@code true} when the IP-scoped failure counter in Redis
     * has reached {@link #MAX_LOGIN_FAILURES} within {@link #LOGIN_FAIL_WINDOW}. Any Redis hiccup is
     * swallowed (fail-open) so an infra outage never blocks legitimate logins.
     */
    private boolean isLockedOut(String failKey) {
        try {
            String count = redisTemplate.opsForValue().get(failKey);
            return count != null && Integer.parseInt(count) >= MAX_LOGIN_FAILURES;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Increments the IP-scoped failure counter, (re)setting the sliding TTL. Best-effort. */
    private void recordLoginFailure(String failKey) {
        try {
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(failKey, LOGIN_FAIL_WINDOW);
            }
        } catch (RuntimeException ignored) {
            // Redis unavailable — skip lockout bookkeeping rather than break login.
        }
    }

    /** Clears the failure counter after a successful login. Best-effort. */
    private void clearLoginFailures(String failKey) {
        try {
            redisTemplate.delete(failKey);
        } catch (RuntimeException ignored) {
            // ignore
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        DecodedJWT jwt = tokenProvider.verify(refreshToken);
        if (!tokenProvider.isRefreshToken(jwt)) {
            throw new ApiException(ResultCode.INVALID_TOKEN);
        }
        Long adminId = Long.valueOf(jwt.getSubject());
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + adminId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new ApiException(ResultCode.INVALID_TOKEN);
        }
        AdminAccount admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ResultCode.INVALID_TOKEN));
        return issueTokens(admin);
    }

    public void logout(String refreshToken) {
        try {
            DecodedJWT jwt = tokenProvider.verify(refreshToken);
            redisTemplate.delete(REFRESH_KEY_PREFIX + jwt.getSubject());
        } catch (ApiException ignored) {
            // already invalid/expired — nothing to revoke
        }
    }

    private TokenResponse issueTokens(AdminAccount admin) {
        String accessToken = tokenProvider.createAccessToken(admin);
        String refreshToken = tokenProvider.createRefreshToken(admin);
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + admin.getId(),
                refreshToken,
                Duration.ofSeconds(tokenProvider.getRefreshExpSeconds()));
        return new TokenResponse(accessToken, refreshToken, tokenProvider.getAccessExpSeconds());
    }
}
