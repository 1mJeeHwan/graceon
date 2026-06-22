package org.streamhub.api.v1.actionlog.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Drains the {@link ActionOutbox} to Kafka. Active when {@code app.eventlog.outbox=true} (the MSA
 * production path — the extracted {@code streamhub-audit-service} consumes the topic). Each tick
 * claims a batch of unpublished rows, publishes each with a <b>confirmed</b> send (waits for the
 * broker ack), then flips published rows in one bulk update; rows whose send failed stay unpublished
 * and are retried next tick (their attempt counter is bumped).
 *
 * <p>Kafka I/O happens outside any DB transaction — the only writes are the short bulk
 * {@code markPublished}/{@code recordFailure} updates. Delivery is at-least-once: a crash after the
 * broker ack but before {@code markPublished} re-publishes the row, which the audit consumer
 * tolerates (an audit trail accepts the occasional duplicate). The published key is the operator id,
 * preserving per-operator ordering on the topic just like the direct transport.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.eventlog.outbox.enabled", havingValue = "true")
public class ActionOutboxRelay {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final ActionOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public ActionOutboxRelay(ActionOutboxRepository outboxRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper,
                             @Value("${app.kafka.action-log-topic}") String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.eventlog.outbox.relay-interval-ms:2000}")
    public void drain() {
        List<ActionOutbox> batch = outboxRepository.findTop100ByPublishedFalseOrderByIdAsc();
        if (batch.isEmpty()) {
            return;
        }
        List<Long> published = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        for (ActionOutbox row : batch) {
            if (publish(row)) {
                published.add(row.getId());
            } else {
                failed.add(row.getId());
            }
        }
        if (!published.isEmpty()) {
            outboxRepository.markPublished(published, LocalDateTime.now());
        }
        if (!failed.isEmpty()) {
            outboxRepository.recordFailure(failed);
        }
        log.debug("Outbox relay: published {}, failed {}", published.size(), failed.size());
    }

    private boolean publish(ActionOutbox row) {
        try {
            ActionLogMessage message = objectMapper.readValue(row.getPayload(), ActionLogMessage.class);
            kafkaTemplate.send(topic, row.getAggregateKey(), message)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Outbox relay interrupted while publishing row {}", row.getId());
            return false;
        } catch (Exception e) {
            log.warn("Outbox relay failed to publish row {} (attempt {}): {}",
                    row.getId(), row.getAttempts() + 1, e.getMessage());
            return false;
        }
    }
}
