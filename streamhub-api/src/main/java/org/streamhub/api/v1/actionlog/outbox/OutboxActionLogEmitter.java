package org.streamhub.api.v1.actionlog.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.actionlog.ActionLogEmitter;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Transactional-outbox emitter. Active when {@code app.eventlog.outbox=true}. Instead of publishing
 * to the broker inline, it persists the event as an {@link ActionOutbox} row <b>in the caller's
 * transaction</b>: if the business change commits, the event is durably enqueued; if it rolls back,
 * the event vanishes with it. The {@link ActionOutboxRelay} publishes the row afterwards.
 *
 * <p>Unlike {@link org.streamhub.api.v1.actionlog.DirectActionLogEmitter}, this path does <b>not</b>
 * swallow failures — a failed outbox insert rolls the business transaction back. That is the
 * deliberate trade: atomic, no-loss event production over best-effort fire-and-forget.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.eventlog.outbox.enabled", havingValue = "true")
public class OutboxActionLogEmitter implements ActionLogEmitter {

    private static final String EVENT_TYPE = "ACTION_LOG";

    private final ActionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxActionLogEmitter(ActionOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void emit(ActionLogMessage message) {
        String payload = serialize(message);
        String key = message.adminId() != null ? String.valueOf(message.adminId()) : null;
        outboxRepository.save(ActionOutbox.of(EVENT_TYPE, key, payload));
    }

    private String serialize(ActionLogMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            // A plain record never fails to serialize; treat as a programming error, not a swallow.
            throw new IllegalStateException("Failed to serialize action event for outbox", e);
        }
    }
}
