package org.streamhub.api.v1.member.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.church.entity.Denomination;
import org.streamhub.api.v1.member.entity.Church;

/** JPA repository for {@link Church}. Listing/location-search uses MyBatis. */
public interface ChurchRepository extends JpaRepository<Church, Long> {

    List<Church> findByUseYn(String useYn);

    List<Church> findByOpenYn(String openYn);

    List<Church> findByRegionId(Long regionId);

    List<Church> findByDenomination(Denomination denomination);

    /** Bounding-box 1st-pass filter for location search (index-assisted, refined by Haversine). */
    List<Church> findByUseYnAndLatitudeBetweenAndLongitudeBetween(
            String useYn, Double minLat, Double maxLat, Double minLng, Double maxLng);
}
