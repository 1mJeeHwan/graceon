package org.streamhub.api.v1.security;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.v1.security.dto.SecurityEventItem;
import org.streamhub.api.v1.security.repository.SecurityEventRepository;

/** Read side of the security log: paginated, most-recent-first list for the admin viewer. */
@Service
public class SecurityEventService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final SecurityEventRepository securityEventRepository;

    public SecurityEventService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    /**
     * Most-recent-first page of security events.
     *
     * <p>Rows carry the login id that was attempted (frequently a real member's email address) and
     * the originating IP. Both are masked for the public read-only demo account: telling an
     * anonymous browser which accounts are under attack, and from where, hands an attacker the
     * defender's view. Real operators see the values in full — triaging an incident needs them.
     */
    @Transactional(readOnly = true)
    public ResInfinityList<SecurityEventItem> list(Integer pageNumber, Integer pageSize,
                                                   AdminPrincipal principal) {
        int size = normalizeSize(pageSize);
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean mask = principal != null && principal.isDemoViewer();
        Page<SecurityEventItem> result = securityEventRepository.findAll(pageable)
                .map(event -> mask
                        ? SecurityEventItem.masked(event)
                        : SecurityEventItem.from(event));
        List<SecurityEventItem> contents = result.getContent();
        return ResInfinityList.of(contents, result.getTotalElements(), size);
    }

    private int normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
