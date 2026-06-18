package org.streamhub.api.v1.goods.category.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create/update input for a goods category. {@code parentId} is null for a root node;
 * {@code depth} is derived by the service. On update only {@code name}, {@code sortOrder}
 * and {@code useYn} are applied.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsCategorySaveRequest {
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private String useYn;
}
