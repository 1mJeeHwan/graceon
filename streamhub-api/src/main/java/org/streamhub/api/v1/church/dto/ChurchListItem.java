package org.streamhub.api.v1.church.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.church.entity.Denomination;

/** One row of the admin church list, joined with the region name. */
@Getter
@Setter
@NoArgsConstructor
public class ChurchListItem {
    private Long id;
    private String name;
    private Denomination denomination;
    private Long regionId;
    private String regionName;
    private String address;
    private String phone;
    private String pastorName;
    private Double latitude;
    private Double longitude;
    private String thumbnailKey;
    private String thumbnailUrl; // filled by the service from thumbnailKey
    private String dataSource;
    private String openYn;
    private String useYn;
    private LocalDateTime createdAt;
}
