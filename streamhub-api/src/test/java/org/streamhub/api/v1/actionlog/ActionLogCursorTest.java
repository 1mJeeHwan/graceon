package org.streamhub.api.v1.actionlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResCursorList;
import org.streamhub.api.v1.actionlog.dto.ActionLogCursorRequest;
import org.streamhub.api.v1.actionlog.dto.ActionLogItem;
import org.streamhub.api.v1.actionlog.mapper.ActionLogMapper;

/**
 * Keyset paging over the audit log: the cursor round-trips the sort key, the reader asks for one row
 * more than the page size to detect a next page, and a client-supplied cursor is rejected rather than
 * silently ignored.
 */
@ExtendWith(MockitoExtension.class)
class ActionLogCursorTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 7, 29, 10, 0);

    @Mock private ActionLogMapper actionLogMapper;

    private static ActionLogItem row(long id, LocalDateTime createdAt) {
        ActionLogItem item = new ActionLogItem();
        item.setId(id);
        item.setCreatedAt(createdAt);
        return item;
    }

    /** Rows newest-first, one second apart — the order the query returns them in. */
    private static List<ActionLogItem> rows(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> row(100L - i, BASE.minusSeconds(i)))
                .toList();
    }

    @Test
    void encodedCursorRoundTripsTheSortKey() {
        ActionLogItem item = row(42L, BASE);

        ActionLogCursorRequest.Key key =
                new ActionLogCursorRequest(ActionLogCursorRequest.encode(item), null, null, null).key();

        assertThat(key.id()).isEqualTo(42L);
        assertThat(key.createdAt()).isEqualTo(BASE);
    }

    @Test
    void firstPageStartsWithoutAKeyAndReportsMoreRows() {
        when(actionLogMapper.selectAfter(isNull(), isNull(), isNull(), isNull(), eq(3)))
                .thenReturn(rows(3)); // size 2 + the probe row

        ResCursorList<ActionLogItem> page = new LocalActionLogReader(actionLogMapper)
                .listAfter(new ActionLogCursorRequest(null, 2, "  ", ""));

        assertThat(page.getContents()).extracting(ActionLogItem::getId).containsExactly(100L, 99L);
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.getNextCursor()).isNotNull();
    }

    @Test
    void nextPageResumesAfterTheLastReturnedRow() {
        when(actionLogMapper.selectAfter(isNull(), isNull(), isNull(), isNull(), eq(3))).thenReturn(rows(3));
        LocalActionLogReader reader = new LocalActionLogReader(actionLogMapper);
        String next = reader.listAfter(new ActionLogCursorRequest(null, 2, null, null)).getNextCursor();

        when(actionLogMapper.selectAfter(isNull(), isNull(), any(), any(), eq(3))).thenReturn(rows(1));
        ResCursorList<ActionLogItem> second =
                reader.listAfter(new ActionLogCursorRequest(next, 2, null, null));

        ArgumentCaptor<LocalDateTime> createdAt = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Long> id = ArgumentCaptor.forClass(Long.class);
        // Two calls were made (page 1 with a null key, page 2 with the cursor); assert on the latter.
        verify(actionLogMapper, times(2))
                .selectAfter(isNull(), isNull(), createdAt.capture(), id.capture(), eq(3));
        assertThat(id.getValue()).isEqualTo(99L); // the last row of page 1, not the probe row
        assertThat(createdAt.getValue()).isEqualTo(BASE.minusSeconds(1));
        assertThat(second.isHasNext()).isFalse();
        assertThat(second.getNextCursor()).isNull();
    }

    @Test
    void lastPageDropsTheCursor() {
        when(actionLogMapper.selectAfter(isNull(), isNull(), isNull(), isNull(), eq(16)))
                .thenReturn(rows(4)); // fewer than the requested 15 + probe

        ResCursorList<ActionLogItem> page = new LocalActionLogReader(actionLogMapper)
                .listAfter(new ActionLogCursorRequest(null, null, null, null));

        assertThat(page.getContents()).hasSize(4);
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getNextCursor()).isNull();
    }

    @Test
    void pageSizeIsCapped() {
        assertThat(new ActionLogCursorRequest(null, 5_000, null, null).pageSizeOrDefault()).isEqualTo(100);
        assertThat(new ActionLogCursorRequest(null, 0, null, null).pageSizeOrDefault()).isEqualTo(15);
    }

    @Test
    void malformedCursorIsRejected() {
        assertThatThrownBy(() -> new ActionLogCursorRequest("!!not-base64!!", null, null, null).key())
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new ActionLogCursorRequest("bm90LWEtcGFpcg", null, null, null).key())
                .isInstanceOf(ApiException.class); // "not-a-pair"
    }
}
