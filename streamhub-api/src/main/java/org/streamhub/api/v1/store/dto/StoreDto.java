package org.streamhub.api.v1.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.store.entity.Store;

/**
 * An offline store row. Used as both the admin create/update input and the list/detail
 * output. {@code distanceKm} is filled only by the public distance-sorted listing. All
 * values are demo/fictional (PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class StoreDto {
    private Long id;
    private Long regionId;
    private String name;
    private String address;
    private String phone;
    private BigDecimal lat;
    private BigDecimal lng;
    private String openHours;
    private String useYn;
    private Double distanceKm; // filled by the public distance-sorted listing
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted store. */
    public static StoreDto from(Store store) {
        StoreDto dto = new StoreDto();
        dto.id = store.getId();
        dto.regionId = store.getRegionId();
        dto.name = store.getName();
        dto.address = store.getAddress();
        dto.phone = store.getPhone();
        dto.lat = store.getLat();
        dto.lng = store.getLng();
        dto.openHours = store.getOpenHours();
        dto.useYn = store.getUseYn();
        dto.createdAt = store.getCreatedAt();
        return dto;
    }
}
