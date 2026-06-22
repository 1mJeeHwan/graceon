package org.streamhub.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StreamHub Audit Service — a standalone microservice extracted from the monolith. Its single
 * responsibility: consume admin-action events from the {@code streamhub-action-log} Kafka topic and
 * persist them. The monolith ({@code streamhub-api}) becomes a producer only
 * ({@code app.eventlog.consume=false}); the two communicate asynchronously through Kafka — no
 * synchronous coupling. See docs/msa-split.md.
 */
@SpringBootApplication
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
