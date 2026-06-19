package org.streamhub.api.base.external.discovery;

import java.util.List;

/**
 * Nearby-church discovery seam (external POI search). Mirrors the
 * {@link org.streamhub.api.base.external.geocode.GeocodeProvider} pattern: the default
 * {@link NoopChurchDiscoveryProvider} returns nothing (DB-only nearby search), while
 * {@link KakaoChurchDiscoveryProvider} surfaces real surrounding churches from Kakao Local,
 * which {@code ChurchService} merges into the DB results (deduped, distance-sorted).
 */
public interface ChurchDiscoveryProvider {

    /**
     * Finds churches near a point. Best-effort: returns an empty list when discovery is disabled
     * or nothing matches.
     *
     * @param lat      origin latitude
     * @param lng      origin longitude
     * @param radiusKm search radius in km
     * @param keyword  optional extra keyword (null/blank → just "교회")
     * @return discovered churches (never null)
     */
    List<DiscoveredChurch> search(double lat, double lng, double radiusKm, String keyword);
}
