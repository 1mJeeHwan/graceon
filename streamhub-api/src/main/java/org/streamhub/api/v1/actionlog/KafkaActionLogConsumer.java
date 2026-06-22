package org.streamhub.api.v1.actionlog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Consumes action events from Kafka and persists them via {@link ActionLogWriter}. Active when
 * {@code app.eventlog.transport=kafka} AND {@code app.eventlog.consume} is not false. Setting
 * {@code app.eventlog.consume=false} makes this app a <b>producer only</b> — the extracted
 * {@code streamhub-audit-service} then owns consumption (MSA split; see docs/msa-split.md).
 * At-least-once delivery (offset committed after a successful write), so a redelivery can re-insert.
 */
@Slf4j
@Component
@ConditionalOnExpression("'${app.eventlog.transport:sqs}' == 'kafka' and '${app.eventlog.consume:true}' == 'true'")
public class KafkaActionLogConsumer {

    private final ActionLogWriter writer;

    public KafkaActionLogConsumer(ActionLogWriter writer) {
        this.writer = writer;
    }

    @KafkaListener(topics = "${app.kafka.action-log-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handle(ActionLogMessage message) {
        writer.write(message);
    }
}
