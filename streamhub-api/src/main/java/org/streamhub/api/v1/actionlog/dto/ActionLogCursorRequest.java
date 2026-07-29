package org.streamhub.api.v1.actionlog.dto;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Keyset (cursor) pagination for the audit log.
 *
 * <p>The audit table is append-only and only ever read newest-first, which is the shape offset
 * pagination handles worst: {@code LIMIT 100000, 15} makes MySQL walk and discard every skipped row,
 * and each page's {@code COUNT(*)} rescans the whole filtered set. A cursor carries the previous
 * page's last sort key instead, so every page is an index range scan of {@code size} rows no matter
 * how deep, and no count is needed. It is also stable while rows arrive: with offset paging a log
 * written mid-scroll pushes every row one position down and the next page repeats what was just read.
 *
 * <p>The cursor is the opaque, URL-safe Base64 of {@code <createdAt epoch millis>:<id>} — exactly the
 * {@code (created_at DESC, id DESC)} sort key, id breaking ties inside the same millisecond. Opaque
 * because clients must not build one: the encoding changes whenever the sort does.
 *
 * @param cursor   last row of the previous page; null/blank starts from the newest row
 * @param pageSize rows to return (default 15, capped at 100)
 * @param action   exact action-code filter
 * @param keyword  matched against adminName / detail / targetId (LIKE)
 */
public record ActionLogCursorRequest(String cursor, Integer pageSize, String action, String keyword) {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    public int pageSizeOrDefault() {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /** Decoded sort key, or null on the first page. */
    public Key key() {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "잘못된 커서입니다");
        }
        String[] parts = decoded.split(":");
        if (parts.length != 2) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "잘못된 커서입니다");
        }
        try {
            LocalDateTime createdAt =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[0])), ZONE);
            return new Key(createdAt, Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "잘못된 커서입니다");
        }
    }

    public String actionOrNull() {
        return action == null || action.isBlank() ? null : action;
    }

    public String keywordOrNull() {
        return keyword == null || keyword.isBlank() ? null : keyword;
    }

    /** Encodes the sort key of {@code item} as the cursor pointing just past it. */
    public static String encode(ActionLogItem item) {
        long millis = item.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((millis + ":" + item.getId()).getBytes(StandardCharsets.UTF_8));
    }

    /** The {@code (created_at, id)} pair a page resumes after. */
    public record Key(LocalDateTime createdAt, long id) {
    }
}
