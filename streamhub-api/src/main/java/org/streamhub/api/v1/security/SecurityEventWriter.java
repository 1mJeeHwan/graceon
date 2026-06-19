package org.streamhub.api.v1.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.security.entity.SecurityEvent;
import org.streamhub.api.v1.security.repository.SecurityEventRepository;

/**
 * Persists a {@link SecurityEvent} in its own writable transaction (REQUIRES_NEW).
 *
 * <p>Security recording is triggered from observed requests that may run in a read-only
 * transaction (e.g. a failed login on a read-only auth service) or outside one (the exception
 * handler). A direct save would then join that read-only transaction and fail with
 * "Connection is read-only". Running the insert in its own transaction guarantees it commits
 * regardless of — and without tainting — the caller's transaction.
 */
@Component
public class SecurityEventWriter {

    private final SecurityEventRepository securityEventRepository;

    public SecurityEventWriter(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(SecurityEvent event) {
        securityEventRepository.save(event);
    }
}
