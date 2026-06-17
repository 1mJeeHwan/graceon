package org.streamhub.api.v1.sms.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the active {@link SmsSender} from all registered beans (C6 seam). The configured
 * {@code app.sms.sender} default is used; in the demo only {@link MockSmsSender} is present
 * (the real adapters are {@code @ConditionalOnProperty}-gated), so it always resolves to mock.
 */
@Component
public class SmsSenderRouter {

    private final Map<String, SmsSender> byCode;
    private final String defaultSender;

    public SmsSenderRouter(List<SmsSender> senders,
                           @Value("${app.sms.sender:mock}") String defaultSender) {
        this.byCode = senders.stream()
                .collect(Collectors.toMap(s -> s.code().toUpperCase(), Function.identity()));
        this.defaultSender = defaultSender == null ? "MOCK" : defaultSender.toUpperCase();
    }

    /** Resolves the configured sender, falling back to MOCK. */
    public SmsSender resolve() {
        SmsSender configured = byCode.get(defaultSender);
        return configured != null ? configured : byCode.get("MOCK");
    }
}
