package org.streamhub.api.v1.actionlog.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transactional-outbox row for an action event. Written in the same DB transaction as the business
 * change that produced it, so the event and the change commit (or roll back) atomically — no lost
 * events on a crash between commit and publish, and no phantom events when the business tx rolls
 * back. A relay ({@link ActionOutboxRelay}) later publishes unpublished rows to Kafka and flips
 * {@link #published}.
 *
 * <p>The payload is the JSON-serialized {@code ActionLogMessage}; {@link #aggregateKey} carries the
 * Kafka partition key (operator id) so per-operator ordering survives the relay.
 */
@Entity
@Table(name = "ACTION_OUTBOX", indexes = {
        @Index(name = "idx_outbox_unpublished", columnList = "published, id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Kafka partition key (operator id as string); null for off-request/system events. */
    @Column(name = "aggregate_key", length = 50)
    private String aggregateKey;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private ActionOutbox(String eventType, String aggregateKey, String payload) {
        this.eventType = eventType;
        this.aggregateKey = aggregateKey;
        this.payload = payload;
        this.published = false;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    /** Creates a new, unpublished outbox row for the given event payload. */
    public static ActionOutbox of(String eventType, String aggregateKey, String payload) {
        return new ActionOutbox(eventType, aggregateKey, payload);
    }
}
