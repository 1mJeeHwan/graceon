package org.streamhub.api.v1.actionlog.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Verifies the outbox emitter persists the event (not publishes it) so it commits with the caller's
 * transaction: one {@link ActionOutbox} row, unpublished, carrying the operator id as the partition
 * key and the JSON-serialized message as payload.
 */
@ExtendWith(MockitoExtension.class)
class OutboxActionLogEmitterTest {

    @Mock private ActionOutboxRepository outboxRepository;

    private OutboxActionLogEmitter emitter() {
        return new OutboxActionLogEmitter(outboxRepository, new ObjectMapper());
    }

    @Test
    void emit_persistsUnpublishedRow_withKeyAndJsonPayload() {
        ActionLogMessage message = new ActionLogMessage(
                7L, "관리자", "MEMBER_APPROVE", "MEMBER", "42", "회원 승인", "1.2.3.4");

        emitter().emit(message);

        ArgumentCaptor<ActionOutbox> captor = ArgumentCaptor.forClass(ActionOutbox.class);
        verify(outboxRepository).save(captor.capture());
        ActionOutbox saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("ACTION_LOG");
        assertThat(saved.getAggregateKey()).isEqualTo("7");
        assertThat(saved.isPublished()).isFalse();
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getPayload())
                .contains("\"action\":\"MEMBER_APPROVE\"")
                .contains("\"adminId\":7");
    }

    @Test
    void emit_nullOperator_persistsNullKey() {
        ActionLogMessage message = new ActionLogMessage(
                null, null, "LOGIN_FAIL", "AUTH", null, "로그인 실패", "1.2.3.4");

        emitter().emit(message);

        ArgumentCaptor<ActionOutbox> captor = ArgumentCaptor.forClass(ActionOutbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getAggregateKey()).isNull();
    }
}
