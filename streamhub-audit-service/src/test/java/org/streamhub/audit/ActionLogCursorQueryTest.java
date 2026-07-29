package org.streamhub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * The audit service's keyset read: {@code size + 1} rows are fetched, the probe row is dropped, and
 * the emitted cursor must decode to the last returned row so the monolith can resume with it.
 */
@ExtendWith(MockitoExtension.class)
class ActionLogCursorQueryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 7, 29, 10, 0);

    @Mock private ActionLogRepository repository;

    private static ActionLog row(long id, LocalDateTime createdAt) {
        ActionLog entity = new ActionLog();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ReflectionTestUtils.setField(entity, "action", "LOGIN");
        return entity;
    }

    private static List<ActionLog> rows(int count) {
        return IntStream.range(0, count).mapToObj(i -> row(100L - i, BASE.minusSeconds(i))).toList();
    }

    @Test
    void dropsTheProbeRowAndCursorsOnTheLastReturnedRow() {
        when(repository.searchAfter(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(rows(3));

        ActionLogCursorPage page = new ActionLogQueryService(repository).listAfter(null, 2, " ", "");

        assertThat(page.contents()).extracting(ActionLogView::id).containsExactly(100L, 99L);
        assertThat(page.hasNext()).isTrue();
        ActionLogCursor decoded = ActionLogCursor.decode(page.nextCursor());
        assertThat(decoded.id()).isEqualTo(99L);
        assertThat(decoded.createdAt()).isEqualTo(BASE.minusSeconds(1));
    }

    @Test
    void lastPageHasNoCursor() {
        when(repository.searchAfter(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(rows(2));

        ActionLogCursorPage page = new ActionLogQueryService(repository).listAfter(null, 5, null, null);

        assertThat(page.contents()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void malformedCursorIsRejectedWith400() {
        assertThatThrownBy(() -> ActionLogCursor.decode("!!nope!!"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
