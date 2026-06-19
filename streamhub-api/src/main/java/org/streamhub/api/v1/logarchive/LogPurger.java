package org.streamhub.api.v1.logarchive;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.actionlog.repository.ActionLogRepository;
import org.streamhub.api.v1.security.repository.SecurityEventRepository;

/**
 * Transactional purge of archived log rows. Kept as a separate bean from
 * {@link LogArchiveService} so each {@code @Modifying} bulk-delete runs inside a real transaction
 * via the Spring proxy (self-invocation from the service would bypass it). Each purge is called
 * only after that family's archive upload has succeeded.
 */
@Component
public class LogPurger {

    private final ActionLogRepository actionLogRepository;
    private final SecurityEventRepository securityEventRepository;

    public LogPurger(ActionLogRepository actionLogRepository,
                     SecurityEventRepository securityEventRepository) {
        this.actionLogRepository = actionLogRepository;
        this.securityEventRepository = securityEventRepository;
    }

    /** Deletes action-log rows older than {@code cutoff}; returns the number removed. */
    @Transactional
    public int purgeActionLogs(LocalDateTime cutoff) {
        return actionLogRepository.deleteByCreatedAtBefore(cutoff);
    }

    /** Deletes security-event rows older than {@code cutoff}. */
    @Transactional
    public void purgeSecurityEvents(LocalDateTime cutoff) {
        securityEventRepository.deleteByCreatedAtBefore(cutoff);
    }
}
