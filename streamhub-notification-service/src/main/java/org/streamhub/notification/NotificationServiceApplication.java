package org.streamhub.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for {@code streamhub-notification-service} — an event-driven microservice extracted
 * from the {@code streamhub-api} monolith. Consumes the {@code streamhub-notification-dispatch} Kafka
 * topic, persists each dispatch in its own DB, and serves the {@code /v1/notification-dispatches}
 * read API. See docs/msa-split.md.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
