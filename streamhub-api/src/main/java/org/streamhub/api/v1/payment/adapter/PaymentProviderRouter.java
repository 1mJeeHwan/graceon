package org.streamhub.api.v1.payment.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the active {@link PaymentProvider} from all registered beans by PG code (C4 seam).
 *
 * <p>Resolution order: an explicit per-request provider code (if a matching bean is registered),
 * then the configured {@code app.payment.provider} default, then {@link MockPaymentProvider}.
 * In the demo only the {@code MOCK} bean is present (the real adapters are
 * {@code @ConditionalOnProperty}-gated), so every request resolves to mock.
 */
@Component
public class PaymentProviderRouter {

    private final Map<String, PaymentProvider> byCode;
    private final String defaultProvider;

    public PaymentProviderRouter(List<PaymentProvider> providers,
                                 @Value("${app.payment.provider:mock}") String defaultProvider) {
        this.byCode = providers.stream()
                .collect(Collectors.toMap(p -> p.code().toUpperCase(), Function.identity()));
        this.defaultProvider = defaultProvider == null ? "MOCK" : defaultProvider.toUpperCase();
    }

    /** Resolves the provider for the requested PG code, falling back to the default then MOCK. */
    public PaymentProvider resolve(String requestedCode) {
        if (requestedCode != null) {
            PaymentProvider exact = byCode.get(requestedCode.toUpperCase());
            if (exact != null) {
                return exact;
            }
        }
        PaymentProvider configured = byCode.get(defaultProvider);
        if (configured != null) {
            return configured;
        }
        return byCode.get("MOCK");
    }
}
