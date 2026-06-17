package org.streamhub.api.v1.chat.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the active {@link ChatProvider} from all registered beans (C5 seam). The configured
 * {@code app.chat.provider} default is used; in the demo only {@link RuleChatProvider} is present
 * (the LLM adapter is {@code @ConditionalOnProperty}-gated), so it always resolves to rule.
 */
@Component
public class ChatProviderRouter {

    private final Map<String, ChatProvider> byCode;
    private final String defaultProvider;

    public ChatProviderRouter(List<ChatProvider> providers,
                              @Value("${app.chat.provider:rule}") String defaultProvider) {
        this.byCode = providers.stream()
                .collect(Collectors.toMap(p -> p.code().toUpperCase(), Function.identity()));
        this.defaultProvider = defaultProvider == null ? "RULE" : defaultProvider.toUpperCase();
    }

    /** Resolves the configured provider, falling back to RULE. */
    public ChatProvider resolve() {
        ChatProvider configured = byCode.get(defaultProvider);
        return configured != null ? configured : byCode.get("RULE");
    }
}
