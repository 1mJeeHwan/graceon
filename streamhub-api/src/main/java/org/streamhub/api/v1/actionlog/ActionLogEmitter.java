package org.streamhub.api.v1.actionlog;

import org.streamhub.api.v1.actionlog.dto.ActionLogMessage;

/**
 * Producer seam for action events. Decides <i>how</i> an event leaves the business transaction that
 * emitted it:
 *
 * <ul>
 *   <li>{@link DirectActionLogEmitter} (default) — hands the event straight to the
 *       {@link ActionLogTransport} (best-effort, fire-and-forget). A messaging outage drops the
 *       event but never breaks the business action.</li>
 *   <li>{@link org.streamhub.api.v1.actionlog.outbox.OutboxActionLogEmitter} — when
 *       {@code app.eventlog.outbox=true}, persists the event to an outbox table <b>in the caller's
 *       transaction</b>, so the event commits atomically with the business change. A relay then
 *       publishes it. Trades the "never break the business action" promise for no lost events.</li>
 * </ul>
 *
 * <p>The {@link ActionLogPublisher} call sites are oblivious to which emitter is active — same
 * config-driven seam philosophy as the transport choice.
 */
public interface ActionLogEmitter {

    /** Emits one action event for asynchronous delivery to the audit consumer. */
    void emit(ActionLogMessage message);
}
