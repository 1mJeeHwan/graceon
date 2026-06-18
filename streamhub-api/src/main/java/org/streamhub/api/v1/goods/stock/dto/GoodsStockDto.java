package org.streamhub.api.v1.goods.stock.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.entity.GoodsItem;
import org.streamhub.api.v1.goods.entity.GoodsStatus;

/**
 * A stock row for the inventory grid. {@code price} is the item's selling price and
 * {@code soldOut} is the "Y"/"N" sold-out flag. Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsStockDto {
    private Long id;
    private String code;
    private String name;
    private Integer stock;
    private Integer notiQty;
    private GoodsStatus status;
    private String soldOut;
    private Long price;

    /** Builds a DTO from a persisted goods item. */
    public static GoodsStockDto from(GoodsItem item) {
        GoodsStockDto dto = new GoodsStockDto();
        dto.id = item.getId();
        dto.code = item.getCode();
        dto.name = item.getName();
        dto.stock = item.getStock();
        dto.notiQty = item.getNotiQty();
        dto.status = item.getStatus();
        dto.soldOut = item.getSoldOut();
        dto.price = item.getPrice();
        return dto;
    }
}
