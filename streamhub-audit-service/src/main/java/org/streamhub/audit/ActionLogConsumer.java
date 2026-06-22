package org.streamhub.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The audit service's reason to exist: consume admin-action events from Kafka and persist them.
 * At-least-once (offset committed after the save), so a redelivery may re-insert — acceptable for an
 * audit trail. Persists exactly what the event carries; no enrichment (this service has no access to
 * the admin domain — the producer is responsible for putting {@code adminName} on the event).
 */
@Slf4j
@Component
public class ActionLogConsumer {

    private final ActionLogRepository repository;

    public ActionLogConsumer(ActionLogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.action-log-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handle(ActionLogMessage message) {
        repository.save(ActionLog.builder()
                .adminId(message.adminId())
                .adminName(message.adminName())
                .action(message.action())
                .targetType(message.targetType())
                .targetId(message.targetId())
                .detail(message.detail())
                .ip(message.ip())
                .build());
        log.info("audit recorded: {} by {}", message.action(), message.adminName());
    }
}
