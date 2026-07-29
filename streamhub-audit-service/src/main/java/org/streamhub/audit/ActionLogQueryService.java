package org.streamhub.audit;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the audit store: filterable, paginated lookups over this service's own data. */
@Service
public class ActionLogQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ActionLogRepository repository;

    public ActionLogQueryService(ActionLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ActionLogPage list(Integer pageNumber, Integer pageSize, String action, String keyword) {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Page<ActionLog> result = repository.search(
                blankToNull(action), blankToNull(keyword), PageRequest.of(page, size));
        return new ActionLogPage(
                result.map(ActionLogView::from).getContent(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Keyset page following {@code cursor}. Fetches {@code size + 1} rows so the extra row answers
     * "is there another page?" without a count query.
     */
    @Transactional(readOnly = true)
    public ActionLogCursorPage listAfter(String cursor, Integer pageSize, String action, String keyword) {
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        ActionLogCursor key = ActionLogCursor.decode(cursor);
        List<ActionLog> rows = repository.searchAfter(
                blankToNull(action),
                blankToNull(keyword),
                key == null ? null : key.createdAt(),
                key == null ? null : key.id(),
                PageRequest.ofSize(size + 1));
        boolean hasNext = rows.size() > size;
        List<ActionLog> page = hasNext ? rows.subList(0, size) : rows;
        return new ActionLogCursorPage(
                page.stream().map(ActionLogView::from).toList(),
                hasNext ? ActionLogCursor.encode(page.get(page.size() - 1)) : null,
                hasNext);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
