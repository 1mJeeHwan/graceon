package org.streamhub.api.v1.actionlog;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.base.util.ClientIpResolver;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Publishes admin-action events to SQS. The consumer ({@link ActionLogConsumer})
 * persists them. Publishing is best-effort: a messaging failure never breaks the
 * underlying business action.
 *
 * <p>The originating client IP is captured here (in the operator's request thread) via
 * {@link ClientIpResolver} and carried on the message, so the audit row records who acted and
 * from where. Off-request publishes (schedulers, startup) carry a null IP.
 */
@Slf4j
@Component
public class ActionLogPublisher {

    private final SqsTemplate sqsTemplate;
    private final ClientIpResolver clientIpResolver;
    private final String queueName;

    public ActionLogPublisher(SqsTemplate sqsTemplate,
                              ClientIpResolver clientIpResolver,
                              @Value("${app.sqs.action-log-queue}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.clientIpResolver = clientIpResolver;
        this.queueName = queueName;
    }

    /** Publish using the currently authenticated operator (from the security context). */
    public void publish(String action, String targetType, String targetId, String detail) {
        AdminPrincipal principal = currentPrincipal();
        Long adminId = principal != null ? principal.id() : null;
        publishAs(adminId, null, action, targetType, targetId, detail);
    }

    /** Publish with an explicit operator (e.g. on login, before the security context exists). */
    public void publishAs(Long adminId, String adminName, String action,
                          String targetType, String targetId, String detail) {
        try {
            sqsTemplate.send(queueName, new ActionLogMessage(
                    adminId, adminName, action, targetType, targetId, detail, currentIp()));
        } catch (RuntimeException e) {
            log.warn("Failed to publish action log [{}]: {}", action, e.getMessage());
        }
    }

    private AdminPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal p) {
            return p;
        }
        return null;
    }

    /** Resolves the current request's client IP, or null when published off a request thread. */
    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return clientIpResolver.resolve(request);
        }
        return null;
    }
}
