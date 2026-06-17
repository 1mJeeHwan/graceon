package org.streamhub.api.v1.store.dto;

/**
 * Store-finder request. When {@code lat}/{@code lng} are given the result is sorted by
 * Haversine distance; otherwise it falls back to a {@code regionId} filter (or all).
 *
 * @param regionId optional region filter (used when no coordinates are given)
 * @param lat      caller latitude (WGS84)
 * @param lng      caller longitude (WGS84)
 */
public record StoreSearchRequest(Long regionId, Double lat, Double lng) {
}
