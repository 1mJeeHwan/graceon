package org.streamhub.api.v1.church.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.church.entity.Denomination;

/**
 * One row of the public location-search result. Base fields come from MyBatis;
 * {@code distanceKm} and {@code thumbnailUrl} are filled by the service.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChurchNearbyItem {
    private Long id;
    private String name;
    private Denomination denomination;
    private Long regionId;
    private String regionName;
    private String address;
    private String phone;
    private String pastorName;
    private String facilities;
    private Double latitude;
    private Double longitude;
    private String thumbnailKey;
    private String thumbnailUrl;
    private String dataSource;
    /**
     * External map deep-link for discovery results ({@code dataSource="KAKAO_POI"}); null for
     * DB-backed churches, which navigate to the internal detail page instead.
     */
    private String externalUrl;
    /** Great-circle distance from the search origin (km); null when no location was supplied. */
    private Double distanceKm;
}
