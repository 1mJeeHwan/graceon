package org.streamhub.api.base.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Surfaces the action-log event pipeline's runtime mode on the health endpoint (key:
 * {@code eventTransport}) so the admin "시스템 상태" page can show which transport is active and
 * whether this instance is a producer-only node (MSA split). Always UP — it reports configuration,
 * not a dependency; broker reachability is checked by {@link KafkaBrokerHealthIndicator}.
 */
@Component
public class EventTransportHealthIndicator implements HealthIndicator {

    private final String transport;
    private final boolean consume;

    public EventTransportHealthIndicator(
            @Value("${app.eventlog.transport:sqs}") String transport,
            @Value("${app.eventlog.consume:true}") boolean consume) {
        this.transport = transport;
        this.consume = consume;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("transport", transport)
                .withDetail("consume", consume)
                .withDetail("role", consume ? "producer+consumer" : "producer-only")
                .build();
    }
}
