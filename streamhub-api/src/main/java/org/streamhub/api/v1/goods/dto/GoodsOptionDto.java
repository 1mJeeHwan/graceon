package org.streamhub.api.v1.goods.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsOption;

/**
 * A goods option row. Used both as a create/update input (dynamic rows) and as a
 * detail output (id populated). Mutable to match the MyBatis/JPA mapping style.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsOptionDto {
    private Long id;
    private String name;
    private String optionType;
    private Long extraPrice;
    private Integer stock;
    private String useYn;
    private Integer sort;

    /** Builds a detail DTO from a persisted option. */
    public static GoodsOptionDto from(GoodsOption option) {
        GoodsOptionDto dto = new GoodsOptionDto();
        dto.id = option.getId();
        dto.name = option.getName();
        dto.optionType = option.getOptionType();
        dto.extraPrice = option.getExtraPrice();
        dto.stock = option.getStock();
        dto.useYn = option.getUseYn();
        dto.sort = option.getSort();
        return dto;
    }
}
