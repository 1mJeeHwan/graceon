package org.streamhub.api.v1.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.streamhub.api.v1.church.entity.Denomination;

/**
 * A church belonging to a {@link Region}; members are scoped to a church.
 *
 * <p>Extended (C1 church-finder) with location/denomination/contact columns. The
 * original four columns ({@code regionId}/{@code name}/{@code openYn}/{@code createdAt})
 * are preserved. Distance is never stored — Haversine is computed in the query/service.
 */
@Entity
@Table(name = "CHURCH", indexes = {
        @Index(name = "idx_church_region", columnList = "region_id"),
        @Index(name = "idx_church_denom", columnList = "denomination"),
        @Index(name = "idx_church_geo", columnList = "latitude, longitude"),
        @Index(name = "idx_church_use", columnList = "use_yn")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Church {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "open_yn", nullable = false, length = 1)
    private String openYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Denomination (감리교/장로교/…). */
    @Enumerated(EnumType.STRING)
    @Column(name = "denomination", length = 20)
    private Denomination denomination;

    /** WGS84 latitude (seed value). */
    @Column(name = "latitude")
    private Double latitude;

    /** WGS84 longitude (seed value). */
    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "zipcode", length = 10)
    private String zipcode;

    /** Masked virtual phone number. */
    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "pastor_name", length = 50)
    private String pastorName;

    /** CSV facility tags (e.g. {@code 주차,승강기,영유아실,카페}). */
    @Column(name = "facilities", length = 200)
    private String facilities;

    @Column(name = "introduction", length = 2000)
    private String introduction;

    @Column(name = "homepage_url", length = 300)
    private String homepageUrl;

    @Column(name = "thumbnail_key", length = 300)
    private String thumbnailKey;

    /** Data origin marker. Seed = {@code "SEED"} (demo-badge basis); real link = {@code "KAKAO"} etc. */
    @Column(name = "data_source", length = 20)
    private String dataSource;

    /** Visibility flag for public search ({@code "Y"}/{@code "N"}). */
    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private Church(Long regionId, String name, String openYn, Denomination denomination,
                   Double latitude, Double longitude, String address, String addressDetail,
                   String zipcode, String phone, String pastorName, String facilities,
                   String introduction, String homepageUrl, String thumbnailKey,
                   String dataSource, String useYn) {
        this.regionId = regionId;
        this.name = name;
        this.openYn = openYn;
        this.denomination = denomination;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipcode = zipcode;
        this.phone = phone;
        this.pastorName = pastorName;
        this.facilities = facilities;
        this.introduction = introduction;
        this.homepageUrl = homepageUrl;
        this.thumbnailKey = thumbnailKey;
        this.dataSource = dataSource != null ? dataSource : "SEED";
        this.useYn = useYn != null ? useYn : "Y";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Updates admin-editable fields. */
    public void update(String name, Long regionId, Denomination denomination, Double latitude,
                       Double longitude, String address, String addressDetail, String zipcode,
                       String phone, String pastorName, String facilities, String introduction,
                       String homepageUrl, String thumbnailKey, String openYn, String useYn) {
        this.name = name;
        this.regionId = regionId;
        this.denomination = denomination;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipcode = zipcode;
        this.phone = phone;
        this.pastorName = pastorName;
        this.facilities = facilities;
        this.introduction = introduction;
        this.homepageUrl = homepageUrl;
        this.thumbnailKey = thumbnailKey;
        this.openYn = openYn;
        this.useYn = useYn;
        this.updatedAt = LocalDateTime.now();
    }

    /** Applies a geocode-seam result (address → coordinates). */
    public void applyGeocode(Double latitude, Double longitude, String source) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.dataSource = source;
        this.updatedAt = LocalDateTime.now();
    }
}
