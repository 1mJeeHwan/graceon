package org.streamhub.api.v1.goods.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsCategory;

/**
 * A goods category node for the shop tree. {@code children} are assembled by the service
 * ({@link org.streamhub.api.v1.goods.GoodsCategoryService}); {@code imageUrl} is resolved
 * from {@code imageKey}.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsCategoryDto {
    private Long id;
    private Long parentId;
    private String name;
    private Integer depth;
    private Integer sort;
    private String imageKey;
    private String imageUrl;
    private String useYn;
    private List<GoodsCategoryDto> children = new ArrayList<>();

    /** Builds a flat node (no children) from a persisted category with its resolved image URL. */
    public static GoodsCategoryDto of(GoodsCategory category, String imageUrl) {
        GoodsCategoryDto dto = new GoodsCategoryDto();
        dto.id = category.getId();
        dto.parentId = category.getParentId();
        dto.name = category.getName();
        dto.depth = category.getDepth();
        dto.sort = category.getSort();
        dto.imageKey = category.getImageKey();
        dto.imageUrl = imageUrl;
        dto.useYn = category.getUseYn();
        return dto;
    }
}
