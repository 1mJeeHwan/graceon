package org.streamhub.api.v1.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.streamhub.api.base.util.ClientIpResolver;
import org.streamhub.api.v1.security.entity.SecurityEvent;
import org.streamhub.api.v1.security.repository.SecurityEventRepository;

/**
 * Records security-monitoring events and runs real-time threshold detection.
 *
 * <p>Recording is best-effort: a persistence failure is logged and swallowed so that security
 * bookkeeping never breaks the request it observes (login, access-denied handling, etc.). The
 * client IP is captured here, on the detecting request thread, via {@link ClientIpResolver} —
 * the same pattern as the audit-log publisher.
 *
 * <p>Depends only on the repository and the IP resolver to stay free of cycles when injected
 * into the global exception handler.
 */
@Slf4j
@Service
public class SecurityMonitor {

    private static final String EVENT_AUTH_FAILURE = "AUTH_FAILURE";
    private static final String EVENT_ACCESS_DENIED = "ACCESS_DENIED";
    private static final String EVENT_SECURITY_ALERT = "SECURITY_ALERT";

    private static final String SEVERITY_LOW = "LOW";
    private static final String SEVERITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_HIGH = "HIGH";

    private final SecurityEventRepository securityEventRepository;
    private final SecurityEventWriter securityEventWriter;
    private final ClientIpResolver clientIpResolver;

    /** Number of AUTH_FAILUREs from one IP within the window that raises a SECURITY_ALERT. */
    private final int authFailureThreshold;

    /** Sliding window, in minutes, over which AUTH_FAILUREs are counted for the threshold. */
    private final int authFailureWindowMinutes;

    public SecurityMonitor(
            SecurityEventRepository securityEventRepository,
            SecurityEventWriter securityEventWriter,
            ClientIpResolver clientIpResolver,
            @Value("${app.security.auth-failure-threshold:5}") int authFailureThreshold,
            @Value("${app.security.auth-failure-window-minutes:10}") int authFailureWindowMinutes) {
        this.securityEventRepository = securityEventRepository;
        this.securityEventWriter = securityEventWriter;
        this.clientIpResolver = clientIpResolver;
        this.authFailureThreshold = authFailureThreshold;
        this.authFailureWindowMinutes = authFailureWindowMinutes;
    }

    /**
     * Records a security event. The client IP is captured internally from the current request.
     * Best-effort: any failure is logged and swallowed.
     */
    public void record(String eventType, String severity, String actorType, Long actorId,
                       String loginId, String path, String detail) {
        String ip = currentIp();
        save(SecurityEvent.builder()
                .eventType(eventType)
                .severity(severity)
                .actorType(actorType)
                .actorId(actorId)
                .loginId(loginId)
                .ip(ip)
                .path(path)
                .detail(detail)
                .build());
    }

    /**
     * Records an authentication failure and runs the real-time brute-force check: when the same
     * IP has produced {@code authFailureThreshold} or more AUTH_FAILUREs within the window, an
     * additional HIGH {@code SECURITY_ALERT} is recorded.
     *
     * @param loginId   the login identifier attempted (may not exist)
     * @param actorType the subject class, e.g. ADMIN or MEMBER
     */
    public void recordAuthFailure(String loginId, String actorType) {
        String ip = currentIp();
        save(SecurityEvent.builder()
                .eventType(EVENT_AUTH_FAILURE)
                .severity(SEVERITY_LOW)
                .actorType(actorType)
                .loginId(loginId)
                .ip(ip)
                .build());
        checkAuthFailureThreshold(ip, actorType);
    }

    /**
     * Records an access-denial (authorization failure) as a MEDIUM event — captures
     * cross-tenant probing and privilege-escalation attempts.
     */
    public void recordAccessDenied(String path, String actorType, Long actorId) {
        record(EVENT_ACCESS_DENIED, SEVERITY_MEDIUM, actorType, actorId, null, path, null);
    }

    private void checkAuthFailureThreshold(String ip, String actorType) {
        if (ip == null) {
            return;
        }
        try {
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(authFailureWindowMinutes);
            long recent = securityEventRepository.countByEventTypeAndIpAndCreatedAtAfter(
                    EVENT_AUTH_FAILURE, ip, windowStart);
            if (recent >= authFailureThreshold) {
                save(SecurityEvent.builder()
                        .eventType(EVENT_SECURITY_ALERT)
                        .severity(SEVERITY_HIGH)
                        .actorType(actorType)
                        .ip(ip)
                        .detail(recent + "건의 인증 실패가 최근 " + authFailureWindowMinutes
                                + "분 내 동일 IP에서 발생했습니다")
                        .build());
            }
        } catch (RuntimeException e) {
            log.warn("Auth-failure threshold check failed for ip {}: {}", ip, e.getMessage());
        }
    }

    private void save(SecurityEvent event) {
        try {
            // REQUIRES_NEW writer: the observed request may run in a read-only transaction
            // (e.g. failed login) or none at all, so the insert needs its own writable transaction.
            securityEventWriter.save(event);
        } catch (RuntimeException e) {
            log.warn("Failed to record security event [{}]: {}", event.getEventType(), e.getMessage());
        }
    }

    /** Resolves the current request's client IP, or null when off a request thread. */
    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return clientIpResolver.resolve(request);
        }
        return null;
    }
}
