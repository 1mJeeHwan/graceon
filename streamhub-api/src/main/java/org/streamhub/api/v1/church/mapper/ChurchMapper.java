package org.streamhub.api.v1.church.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.streamhub.api.v1.church.dto.ChurchDetail;
import org.streamhub.api.v1.church.dto.ChurchListItem;
import org.streamhub.api.v1.church.dto.ChurchNearbyItem;

/**
 * MyBatis mapper for church queries (region join + dynamic filters + bounding-box pre-filter).
 * Maps to {@code resources/mappers/ChurchMapper.xml}. Distance is computed in the service
 * (Haversine), not in SQL.
 */
@Mapper
public interface ChurchMapper {

    /**
     * Admin list: churches matching the filters, DB-paged. {@code ownChurchId} confines a
     * CHURCH_MANAGER to its own church ({@code null} = unscoped, SYSTEM/VIEWER).
     */
    List<ChurchListItem> selectList(
            @Param("keyword") String keyword,
            @Param("regionId") Long regionId,
            @Param("denomination") String denomination,
            @Param("useYn") String useYn,
            @Param("ownChurchId") Long ownChurchId,
            @Param("offset") int offset,
            @Param("size") int size);

    long countList(
            @Param("keyword") String keyword,
            @Param("regionId") Long regionId,
            @Param("denomination") String denomination,
            @Param("useYn") String useYn,
            @Param("ownChurchId") Long ownChurchId);

    /** Public list (no location): visible churches by region/denomination/keyword, DB-paged. */
    List<ChurchNearbyItem> selectPublicList(
            @Param("keyword") String keyword,
            @Param("regionId") Long regionId,
            @Param("denomination") String denomination,
            @Param("offset") int offset,
            @Param("size") int size);

    long countPublicList(
            @Param("keyword") String keyword,
            @Param("regionId") Long regionId,
            @Param("denomination") String denomination);

    /**
     * Bounding-box pre-filter for location search (index-assisted; refined by Haversine in the
     * service). Ordered by approximate distance from the centre and capped, so a wide radius in a
     * dense area cannot return an unbounded candidate set.
     *
     * @param maxCandidates hard cap on rows returned; the nearest ones are kept
     */
    List<ChurchNearbyItem> selectInBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("maxCandidates") int maxCandidates,
            @Param("keyword") String keyword,
            @Param("regionId") Long regionId,
            @Param("denomination") String denomination);

    /** Base detail (region name). Worship times are loaded by the service. */
    ChurchDetail selectDetail(@Param("id") Long id);
}
