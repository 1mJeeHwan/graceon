package org.streamhub.api.base.util;

import java.util.Map;

/**
 * Builds a safe SQL {@code ORDER BY} body for list endpoints from a client-supplied sort key and
 * direction.
 *
 * <p>Only keys present in the per-endpoint {@code allowedColumns} whitelist are honored; an unknown
 * or absent key falls back to {@code defaultOrderBy}. The returned string is composed solely from
 * server-controlled column literals (the whitelist values) and a fixed {@code ASC}/{@code DESC} —
 * never from raw client input — so it is safe to interpolate into a MyBatis {@code ${orderBy}}.
 * A unique-column tiebreaker is always appended so pagination stays deterministic across rows with
 * equal sort values.
 */
public final class SortResolver {

    private SortResolver() {
    }

    /**
     * @param sortBy         client sort key (e.g. {@code "total"}); resolved via {@code allowedColumns}
     * @param sortDir        {@code "asc"}/{@code "desc"} (case-insensitive); anything else → DESC
     * @param allowedColumns allowed sort key → real column expression (e.g. {@code "o.total"})
     * @param tiebreaker     unique column appended for stable ordering (e.g. {@code "o.id"})
     * @param defaultOrderBy fallback body used when {@code sortBy} is null/unknown (e.g.
     *                       {@code "o.ordered_at DESC, o.id DESC"})
     * @return an {@code ORDER BY} body (without the {@code ORDER BY} keyword)
     */
    public static String resolve(String sortBy, String sortDir,
                                 Map<String, String> allowedColumns,
                                 String tiebreaker, String defaultOrderBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultOrderBy;
        }
        String column = allowedColumns.get(sortBy);
        if (column == null) {
            return defaultOrderBy;
        }
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        return column + " " + dir + ", " + tiebreaker + " DESC";
    }
}
