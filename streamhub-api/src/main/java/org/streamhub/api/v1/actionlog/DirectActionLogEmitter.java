package org.streamhub.api.v1.actionlog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Default emitter — publishes the event straight to the active {@link ActionLogTransport}
 * (SQS or Kafka). Best-effort: the transport swallows failures, so a messaging outage can drop the
 * event but never breaks the business transaction. Active unless {@code app.eventlog.outbox=true},
 * in which case {@link org.streamhub.api.v1.actionlog.outbox.OutboxActionLogEmitter} takes over.
 */
@Component
@ConditionalOnProperty(name = "app.eventlog.outbox.enabled", havingValue = "false", matchIfMissing = true)
public class DirectActionLogEmitter implements ActionLogEmitter {

    private final ActionLogTransport transport;

    public DirectActionLogEmitter(ActionLogTransport transport) {
        this.transport = transport;
    }

    @Override
    public void emit(ActionLogMessage message) {
        transport.send(message);
    }
}
