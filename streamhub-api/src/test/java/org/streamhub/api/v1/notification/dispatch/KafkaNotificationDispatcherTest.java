package org.streamhub.api.v1.notification.dispatch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * The Kafka dispatcher publishes keyed by channel and is best-effort: a broker failure is swallowed
 * so recording a notification never fails because messaging is down.
 */
class KafkaNotificationDispatcherTest {

    private static final String TOPIC = "streamhub-notification-dispatch";

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate =
            org.mockito.Mockito.mock(KafkaTemplate.class);

    private NotificationDispatchEvent event() {
        return new NotificationDispatchEvent("SMS", "BROADCAST", "전체 회원",
                "예배 안내", "주일 예배", "SUCCESS", LocalDateTime.now());
    }

    @Test
    void dispatch_publishesKeyedByChannel() {
        new KafkaNotificationDispatcher(kafkaTemplate, TOPIC).dispatch(event());

        verify(kafkaTemplate).send(eq(TOPIC), eq("SMS"), any(NotificationDispatchEvent.class));
    }

    @Test
    void dispatch_swallowsBrokerFailure() {
        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("broker down"));

        assertThatCode(() -> new KafkaNotificationDispatcher(kafkaTemplate, TOPIC).dispatch(event()))
                .doesNotThrowAnyException();
    }
}
