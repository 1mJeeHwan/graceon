package org.streamhub.api.base.response;

import java.util.List;
import java.util.function.Function;
import lombok.Getter;

/**
 * Keyset-paginated list payload: the rows plus the cursor that resumes after them.
 *
 * <p>Deliberately has no {@code totalCount} — dropping the {@code COUNT(*)} is half the point of
 * keyset paging. {@code hasNext} is derived by asking the query for one row more than requested and
 * dropping it, which answers "is there more?" without counting anything.
 *
 * @param <T> element type
 */
@Getter
public class ResCursorList<T> {

    private final List<T> contents;
    private final String nextCursor;
    private final boolean hasNext;

    private ResCursorList(List<T> contents, String nextCursor, boolean hasNext) {
        this.contents = contents;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    /**
     * Builds a page from {@code rows}, which must have been fetched with {@code size + 1} as the
     * limit. The extra row is not returned; it only proves another page exists.
     *
     * @param rows        rows fetched with limit {@code size + 1}
     * @param size        page size the client asked for
     * @param cursorOfLast cursor encoder for the last returned row
     */
    public static <T> ResCursorList<T> of(List<T> rows, int size, Function<T, String> cursorOfLast) {
        boolean hasNext = rows.size() > size;
        List<T> contents = hasNext ? List.copyOf(rows.subList(0, size)) : List.copyOf(rows);
        String nextCursor = hasNext ? cursorOfLast.apply(contents.get(contents.size() - 1)) : null;
        return new ResCursorList<>(contents, nextCursor, hasNext);
    }

    /**
     * Wraps a page another service already trimmed and cursored (the MSA audit-service read path),
     * where {@code nextCursor} is authoritative and no trailing row is present to inspect.
     */
    public static <T> ResCursorList<T> ofRemote(List<T> contents, String nextCursor) {
        return new ResCursorList<>(List.copyOf(contents), nextCursor, nextCursor != null);
    }
}
