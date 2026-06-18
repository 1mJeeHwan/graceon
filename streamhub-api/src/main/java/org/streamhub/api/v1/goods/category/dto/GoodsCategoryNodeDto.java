package org.streamhub.api.v1.goods.category.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsCategory;

/**
 * A flat goods-category row carrying its parent reference and depth so the frontend can
 * render the 3-tier category tree. Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsCategoryNodeDto {
    private Long id;
    private Long parentId;
    private String name;
    private Integer depth;
    private Integer sortOrder;
    private String useYn;

    /** Builds a flat node from a persisted category ({@code sort} is exposed as {@code sortOrder}). */
    public static GoodsCategoryNodeDto from(GoodsCategory category) {
        GoodsCategoryNodeDto dto = new GoodsCategoryNodeDto();
        dto.id = category.getId();
        dto.parentId = category.getParentId();
        dto.name = category.getName();
        dto.depth = category.getDepth();
        dto.sortOrder = category.getSort();
        dto.useYn = category.getUseYn();
        return dto;
    }
}
