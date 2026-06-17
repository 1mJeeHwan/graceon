package org.streamhub.api.v1.church.dto;

import org.streamhub.api.v1.church.entity.Denomination;

/**
 * Public location-based church search request. When {@code lat}/{@code lng}/{@code radiusKm}
 * are all present, results are bounding-box pre-filtered then distance-sorted (Haversine);
 * otherwise (location denied/unavailable) only region/denomination/keyword filters apply with
 * a {@code createdAt desc} order.
 *
 * @param lat          current latitude (optional)
 * @param lng          current longitude (optional)
 * @param radiusKm     search radius in km (optional; only used with lat/lng)
 * @param denomination denomination filter (optional)
 * @param keyword      name/address keyword (optional)
 * @param regionId     region filter (optional)
 * @param pageNumber   zero-based page index
 * @param pageSize     page size (defaults to 10)
 */
public record ChurchNearbyRequest(
        Double lat,
        Double lng,
        Double radiusKm,
        Denomination denomination,
        String keyword,
        Long regionId,
        Integer pageNumber,
        Integer pageSize) {

    /** Default radius (km) used when lat/lng are supplied but radius is not. */
    private static final double DEFAULT_RADIUS_KM = 5.0;

    public boolean hasLocation() {
        return lat != null && lng != null;
    }

    public double radiusKmOrDefault() {
        return radiusKm == null || radiusKm <= 0 ? DEFAULT_RADIUS_KM : radiusKm;
    }

    public int pageSizeOrDefault() {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    public int pageOrZero() {
        return pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
    }

    public int offset() {
        return pageOrZero() * pageSizeOrDefault();
    }
}
