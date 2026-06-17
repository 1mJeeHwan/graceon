package org.streamhub.api.v1.church.geo;

/**
 * Pure great-circle distance helper (Haversine). Used for location-based church search:
 * a bounding-box pre-filter (index-assisted, in MyBatis) narrows candidates, then this
 * computes the precise distance for the radius cut and distance-ordering in the service.
 *
 * <p>Distance is never persisted on the entity — it is derived per request.
 */
public final class HaversineDistance {

    /** Mean Earth radius in kilometres. */
    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineDistance() {
    }

    /**
     * Great-circle distance in kilometres between two WGS84 coordinates.
     *
     * @param lat1 first latitude (degrees)
     * @param lng1 first longitude (degrees)
     * @param lat2 second latitude (degrees)
     * @param lng2 second longitude (degrees)
     * @return distance in kilometres
     */
    public static double km(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
