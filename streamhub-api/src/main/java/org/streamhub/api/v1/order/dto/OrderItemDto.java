package org.streamhub.api.v1.order.dto;

import org.streamhub.api.v1.order.entity.OrderItem;

/** A single order line (cart snapshot) for the order detail view. */
public record OrderItemDto(
        Long id,
        Long goodsId,
        Long optionId,
        String goodsName,
        String optionName,
        Long unitPrice,
        Integer qty,
        Long lineTotal) {

    /** Maps an {@link OrderItem} entity to its DTO. */
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getGoodsId(),
                item.getOptionId(),
                item.getGoodsName(),
                item.getOptionName(),
                item.getUnitPrice(),
                item.getQty(),
                item.getLineTotal());
    }
}
