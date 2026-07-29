package org.streamhub.audit;

import java.util.List;

/**
 * Keyset-paged audit response. Mirrors the monolith's {@code ResCursorList} shape
 * ({@code contents}/{@code nextCursor}/{@code hasNext}) so the caller deserializes it directly.
 * No {@code totalCount}: skipping the count is the point of keyset paging.
 */
public record ActionLogCursorPage(List<ActionLogView> contents, String nextCursor, boolean hasNext) {
}
