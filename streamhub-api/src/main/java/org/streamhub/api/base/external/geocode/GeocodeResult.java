package org.streamhub.api.base.external.geocode;

/**
 * Result of an address → coordinate lookup.
 *
 * @param latitude  WGS84 latitude
 * @param longitude WGS84 longitude
 * @param source    origin marker ({@code "SEED"} for the demo provider, {@code "KAKAO"} etc. for a real one)
 * @param demo      {@code true} when the coordinates are demo/test data (no real external lookup)
 */
public record GeocodeResult(double latitude, double longitude, String source, boolean demo) {
}
