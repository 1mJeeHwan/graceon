package org.streamhub.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the dispatch store: filterable, paginated lookups over this service's own data. */
@Service
public class NotificationDispatchQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final NotificationDispatchRepository repository;

    public NotificationDispatchQueryService(NotificationDispatchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationDispatchPage list(Integer pageNumber, Integer pageSize,
                                         String channel, String status, String keyword) {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Page<NotificationDispatch> result = repository.search(
                blankToNull(channel), blankToNull(status), blankToNull(keyword), PageRequest.of(page, size));
        return new NotificationDispatchPage(
                result.map(NotificationDispatchView::from).getContent(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
