package org.streamhub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway edge for the StreamHub topology. A single entry point that routes by path to
 * the monolith ({@code streamhub-api}) and the extracted services ({@code streamhub-audit-service},
 * {@code streamhub-notification-service}). Routes are declared in {@code application.yml}. See
 * docs/msa-split.md.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
