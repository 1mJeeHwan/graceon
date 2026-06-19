package org.streamhub.api.v1.security;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.response.ResInfinityList;
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

    @Transactional(readOnly = true)
    public ResInfinityList<SecurityEventItem> list(Integer pageNumber, Integer pageSize) {
        int size = normalizeSize(pageSize);
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SecurityEventItem> result =
                securityEventRepository.findAll(pageable).map(SecurityEventItem::from);
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
