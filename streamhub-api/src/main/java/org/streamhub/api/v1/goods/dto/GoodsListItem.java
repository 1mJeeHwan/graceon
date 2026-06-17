package org.streamhub.api.v1.goods.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsStatus;

/** One row of the goods list, joined with category name and option count. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsListItem {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String code;
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
    private String thumbnailUrl; // filled by the service from thumbnailKey
    private String badges; // comma-joined display badges
    private Integer optionCount; // sub-select count of GOODS_OPTION
    private LocalDateTime createdAt;
}
