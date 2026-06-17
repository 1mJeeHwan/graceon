package org.streamhub.api.v1.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An offline retail store (C3 store-finder). Follows the Church/Region pattern with
 * coordinates. All values are demo/fictional (no real business data — PII guard).
 */
@Entity
@Table(name = "STORE", indexes = {
        @Index(name = "idx_store_region", columnList = "region_id"),
        @Index(name = "idx_store_use", columnList = "use_yn")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → REGION. */
    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    /** WGS84 latitude (demo value). */
    @Column(name = "lat", precision = 10, scale = 7)
    private BigDecimal lat;

    /** WGS84 longitude (demo value). */
    @Column(name = "lng", precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "open_hours", length = 120)
    private String openHours;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Store(Long regionId, String name, String address, String phone, BigDecimal lat,
                  BigDecimal lng, String openHours, String useYn, LocalDateTime createdAt) {
        this.regionId = regionId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.lat = lat;
        this.lng = lng;
        this.openHours = openHours;
        this.useYn = useYn != null ? useYn : "Y";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** Updates editable fields. */
    public void update(Long regionId, String name, String address, String phone, BigDecimal lat,
                       BigDecimal lng, String openHours, String useYn) {
        this.regionId = regionId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.lat = lat;
        this.lng = lng;
        this.openHours = openHours;
        this.useYn = useYn;
    }
}
