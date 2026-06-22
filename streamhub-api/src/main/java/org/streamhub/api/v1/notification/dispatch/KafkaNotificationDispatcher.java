package org.streamhub.api.v1.notification.dispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link NotificationDispatchEvent}s to Kafka for {@code streamhub-notification-service}.
 * Active when {@code app.notification.dispatch.enabled=true}. Keyed by channel so events for one
 * channel keep their relative order on the topic. Best-effort: a send failure is logged, never
 * thrown — recording a notification must not fail because the broker is down. (The audit pipeline's
 * Transactional Outbox is the reference for making this no-loss too; see docs/msa-split.md.)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notification.dispatch.enabled", havingValue = "true")
public class KafkaNotificationDispatcher implements NotificationDispatcher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaNotificationDispatcher(KafkaTemplate<String, Object> kafkaTemplate,
                                       @Value("${app.notification.dispatch.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void dispatch(NotificationDispatchEvent event) {
        try {
            kafkaTemplate.send(topic, event.channel(), event);
        } catch (RuntimeException e) {
            log.warn("Failed to publish notification dispatch [{}]: {}", event.title(), e.getMessage());
        }
    }
}
