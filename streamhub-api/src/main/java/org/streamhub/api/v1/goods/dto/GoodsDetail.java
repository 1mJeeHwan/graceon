package org.streamhub.api.v1.goods.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsStatus;

/** Full goods detail. Base fields from MyBatis; options/images/urls filled by the service. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsDetail {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String code;
    private String description;
    private Long price;
    private Long listPrice;
    private Integer stock;
    private Integer notiQty;
    private String soldOut;
    private String useYn;
    private GoodsStatus status;
    private Integer saleCount;
    private Long viewCount;
    private String thumbnailKey;
    private String thumbnailUrl;
    private String badges;
    private List<GoodsOptionDto> options;
    private List<GoodsImageDto> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
