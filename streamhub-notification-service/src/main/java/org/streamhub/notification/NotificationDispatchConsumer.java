package org.streamhub.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * This service's reason to exist: consume notification-dispatch events from Kafka and persist them.
 * At-least-once (offset committed after the save), so a redelivery may re-insert — acceptable for a
 * dispatch log. Persists exactly what the event carries (no enrichment — this service has no access
 * to the monolith's domain).
 */
@Slf4j
@Component
public class NotificationDispatchConsumer {

    private final NotificationDispatchRepository repository;

    public NotificationDispatchConsumer(NotificationDispatchRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.notification-dispatch-topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handle(NotificationDispatchMessage message) {
        repository.save(NotificationDispatch.builder()
                .channel(message.channel())
                .scope(message.scope())
                .targetMasked(message.targetMasked())
                .title(message.title())
                .content(message.content())
                .status(message.status())
                .sentAt(message.sentAt())
                .build());
        log.info("notification dispatch recorded: {} via {}", message.title(), message.channel());
    }
}
