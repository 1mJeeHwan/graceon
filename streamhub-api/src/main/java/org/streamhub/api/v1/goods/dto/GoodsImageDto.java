package org.streamhub.api.v1.goods.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsImage;

/**
 * A goods gallery image row. Used as a create/update input (s3Key + sort) and as a
 * detail output ({@code url} filled by the service).
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsImageDto {
    private Long id;
    private String s3Key;
    private String url; // filled by the service from s3Key
    private Integer sort;

    /** Builds a detail DTO from a persisted image with its resolved public URL. */
    public static GoodsImageDto of(GoodsImage image, String url) {
        GoodsImageDto dto = new GoodsImageDto();
        dto.id = image.getId();
        dto.s3Key = image.getS3Key();
        dto.url = url;
        dto.sort = image.getSort();
        return dto;
    }
}
