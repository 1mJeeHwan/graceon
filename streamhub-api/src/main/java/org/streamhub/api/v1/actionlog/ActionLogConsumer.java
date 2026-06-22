package org.streamhub.api.v1.actionlog;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Consumes action events from SQS and persists them via {@link ActionLogWriter}. Active on the SQS
 * transport (default) AND when {@code app.eventlog.consume} is not false. The Kafka path uses
 * {@link KafkaActionLogConsumer}; {@code app.eventlog.consume=false} disables in-app consumption so
 * a separate service can own it (MSA split).
 */
@Slf4j
@Component
@ConditionalOnExpression("'${app.eventlog.transport:sqs}' == 'sqs' and '${app.eventlog.consume:true}' == 'true'")
public class ActionLogConsumer {

    private final ActionLogWriter writer;

    public ActionLogConsumer(ActionLogWriter writer) {
        this.writer = writer;
    }

    @SqsListener("${app.sqs.action-log-queue}")
    public void handle(ActionLogMessage message) {
        writer.write(message);
    }
}
