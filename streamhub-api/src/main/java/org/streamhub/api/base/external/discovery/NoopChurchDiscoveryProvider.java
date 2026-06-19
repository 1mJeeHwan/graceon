package org.streamhub.api.base.external.discovery;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default discovery provider — returns no external results, so the nearby search stays DB-only
 * (existing behaviour). Active when {@code church.discovery.provider=noop} or the property is
 * absent ({@code matchIfMissing=true}). Replace with {@link KakaoChurchDiscoveryProvider} by
 * setting {@code church.discovery.provider=kakao}.
 */
@Component
@ConditionalOnProperty(name = "church.discovery.provider", havingValue = "noop", matchIfMissing = true)
public class NoopChurchDiscoveryProvider implements ChurchDiscoveryProvider {

    @Override
    public List<DiscoveredChurch> search(double lat, double lng, double radiusKm, String keyword) {
        return Collections.emptyList();
    }
}
