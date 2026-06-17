package org.streamhub.api.base.external.geocode;

/**
 * Address → coordinate seam (geocoding). The default {@link SeedGeocodeProvider} returns
 * deterministic demo coordinates with no external call. A real implementation
 * ({@link KakaoGeocodeProvider}) can be injected later via {@code church.geocode.provider=kakao}
 * without touching {@code ChurchService} — mirroring the {@code StorageService} endpoint seam.
 *
 * <p>Seed churches already carry coordinates; this seam covers the case where an admin
 * registers a church with only an address and the coordinates must be derived.
 */
public interface GeocodeProvider {

    /**
     * Resolves a postal/road address to coordinates. With no external key configured the
     * default provider returns deterministic demo coordinates ({@code demo=true}).
     *
     * @param address road/postal address (may be null/blank)
     * @return the resolved coordinates and origin marker
     */
    GeocodeResult geocode(String address);
}
