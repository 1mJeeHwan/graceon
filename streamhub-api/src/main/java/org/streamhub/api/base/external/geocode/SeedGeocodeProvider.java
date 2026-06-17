package org.streamhub.api.base.external.geocode;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default geocode provider (demo/test mode). Performs <b>no external call</b>: it hashes the
 * address string into a deterministic coordinate inside the Seoul/Gyeonggi bounding box and
 * returns {@code source="SEED"}, {@code demo=true}. Deterministic — the same address always
 * yields the same point, so seeds/restarts are stable.
 *
 * <p>Active when {@code church.geocode.provider=seed} or the property is absent
 * ({@code matchIfMissing=true}). Replace with {@link KakaoGeocodeProvider} by setting
 * {@code church.geocode.provider=kakao}.
 */
@Component
@ConditionalOnProperty(name = "church.geocode.provider", havingValue = "seed", matchIfMissing = true)
public class SeedGeocodeProvider implements GeocodeProvider {

    /** Source marker for demo coordinates (drives the demo badge). */
    private static final String SOURCE_SEED = "SEED";

    // Seoul/Gyeonggi demo bounding box.
    private static final double MIN_LAT = 37.42;
    private static final double MAX_LAT = 37.70;
    private static final double MIN_LNG = 126.80;
    private static final double MAX_LNG = 127.18;

    @Override
    public GeocodeResult geocode(String address) {
        String key = address == null ? "" : address.trim();
        int hash = key.hashCode();
        // Two independent unsigned fractions in [0,1) from the hash halves.
        double latFraction = ((hash & 0x0000FFFF)) / 65536.0;
        double lngFraction = ((hash >>> 16) & 0x0000FFFF) / 65536.0;
        double latitude = MIN_LAT + latFraction * (MAX_LAT - MIN_LAT);
        double longitude = MIN_LNG + lngFraction * (MAX_LNG - MIN_LNG);
        return new GeocodeResult(round6(latitude), round6(longitude), SOURCE_SEED, true);
    }

    private double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
