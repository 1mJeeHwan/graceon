package org.streamhub.api.base.health;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Reports Kafka broker reachability on the health endpoint (key: {@code kafkaBroker}). Active only
 * when {@code app.eventlog.transport=kafka}. Opens a short-lived AdminClient and describes the
 * cluster with a 3s timeout — UP with broker count + cluster id, or DOWN with the error. Defensive
 * (any failure → DOWN) so the health endpoint itself never throws.
 */
@Component
@ConditionalOnExpression("'${app.eventlog.transport:sqs}' == 'kafka'")
public class KafkaBrokerHealthIndicator implements HealthIndicator {

    private static final int TIMEOUT_SECONDS = 3;

    private final KafkaAdmin kafkaAdmin;

    public KafkaBrokerHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        Map<String, Object> config = kafkaAdmin.getConfigurationProperties();
        try (AdminClient admin = AdminClient.create(config)) {
            DescribeClusterResult cluster = admin.describeCluster();
            int brokers = cluster.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();
            String clusterId = cluster.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return Health.up()
                    .withDetail("brokers", brokers)
                    .withDetail("clusterId", clusterId)
                    .build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
