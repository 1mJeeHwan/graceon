package org.streamhub.api.v1.notification.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default no-op dispatcher — active unless {@code app.notification.dispatch.enabled=true}. Keeps the
 * monolith's notification domain self-contained when the notification microservice is not deployed.
 */
@Component
@ConditionalOnProperty(name = "app.notification.dispatch.enabled", havingValue = "false", matchIfMissing = true)
public class NoopNotificationDispatcher implements NotificationDispatcher {

    @Override
    public void dispatch(NotificationDispatchEvent event) {
        // intentionally no-op: notification service not in use
    }
}
