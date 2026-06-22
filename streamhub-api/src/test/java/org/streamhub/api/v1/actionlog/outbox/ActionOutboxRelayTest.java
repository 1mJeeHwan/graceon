package org.streamhub.api.v1.actionlog.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies the relay's drain semantics: confirmed sends are marked published in one bulk update,
 * a failed send leaves the row unpublished and bumps its attempt counter, and an empty backlog
 * touches neither Kafka nor the store.
 */
@ExtendWith(MockitoExtension.class)
class ActionOutboxRelayTest {

    private static final String TOPIC = "streamhub-action-log";

    @Mock private ActionOutboxRepository outboxRepository;
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);

    private ActionOutboxRelay relay() {
        return new ActionOutboxRelay(outboxRepository, kafkaTemplate, new ObjectMapper(), TOPIC);
    }

    private ActionOutbox row(long id, String key) {
        ActionOutbox r = ActionOutbox.of("ACTION_LOG", key,
                "{\"adminId\":7,\"adminName\":\"관리자\",\"action\":\"MEMBER_APPROVE\","
                        + "\"targetType\":\"MEMBER\",\"targetId\":\"42\",\"detail\":\"d\",\"ip\":\"1.2.3.4\"}");
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    @Test
    void drain_confirmedSends_areMarkedPublished() {
        when(outboxRepository.findTop100ByPublishedFalseOrderByIdAsc())
                .thenReturn(List.of(row(1L, "7"), row(2L, "7")));
        when(kafkaTemplate.send(eq(TOPIC), eq("7"), any()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, Object>) null));

        relay().drain();

        verify(outboxRepository).markPublished(eq(List.of(1L, 2L)), any());
        verify(outboxRepository, never()).recordFailure(anyList());
    }

    @Test
    void drain_failedSend_recordsFailure_andStaysUnpublished() {
        when(outboxRepository.findTop100ByPublishedFalseOrderByIdAsc())
                .thenReturn(List.of(row(1L, "7")));
        when(kafkaTemplate.send(eq(TOPIC), eq("7"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay().drain();

        verify(outboxRepository).recordFailure(eq(List.of(1L)));
        verify(outboxRepository, never()).markPublished(anyList(), any());
    }

    @Test
    void drain_emptyBacklog_touchesNothing() {
        when(outboxRepository.findTop100ByPublishedFalseOrderByIdAsc()).thenReturn(List.of());

        relay().drain();

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(outboxRepository, never()).markPublished(anyList(), any());
        verify(outboxRepository, never()).recordFailure(anyList());
    }
}
