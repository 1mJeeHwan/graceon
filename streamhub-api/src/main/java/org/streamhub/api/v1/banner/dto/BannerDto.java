package org.streamhub.api.v1.banner.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.banner.entity.Banner;
import org.streamhub.api.v1.banner.entity.BannerDevice;
import org.streamhub.api.v1.banner.entity.BannerPosition;

/**
 * A banner row. Used as both the admin create/update input and the list/detail output. All
 * values are demo/fictional. Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class BannerDto {
    private Long id;
    private String title;
    private BannerPosition position;
    private BannerDevice device;
    private String imageUrl;
    private String linkUrl;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int sortOrder;
    private String useYn;
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted banner. */
    public static BannerDto from(Banner banner) {
        BannerDto dto = new BannerDto();
        dto.id = banner.getId();
        dto.title = banner.getTitle();
        dto.position = banner.getPosition();
        dto.device = banner.getDevice();
        dto.imageUrl = banner.getImageUrl();
        dto.linkUrl = banner.getLinkUrl();
        dto.startAt = banner.getStartAt();
        dto.endAt = banner.getEndAt();
        dto.sortOrder = banner.getSortOrder();
        dto.useYn = banner.getUseYn();
        dto.createdAt = banner.getCreatedAt();
        return dto;
    }
}
