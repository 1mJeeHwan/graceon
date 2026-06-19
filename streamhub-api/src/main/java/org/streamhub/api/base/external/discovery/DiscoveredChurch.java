package org.streamhub.api.base.external.discovery;

/**
 * One church discovered via an external POI provider (e.g. Kakao Local). Carries only what a POI
 * search returns — no denomination, worship times, or internal id. {@code externalId} is the
 * provider's place id and {@code placeUrl} deep-links to the provider's map page.
 *
 * @param externalId provider place id (e.g. Kakao place id)
 * @param name       place name
 * @param latitude   WGS84 latitude
 * @param longitude  WGS84 longitude
 * @param address    road/lot address (nullable)
 * @param phone      phone number (nullable)
 * @param category   short category label (nullable)
 * @param placeUrl   provider map deep-link (nullable)
 */
public record DiscoveredChurch(
        String externalId,
        String name,
        double latitude,
        double longitude,
        String address,
        String phone,
        String category,
        String placeUrl) {
}
