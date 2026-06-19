package org.streamhub.api.v1.pub.goods.dto;

/**
 * One row of the public goods (merchandise) storefront list. Only on-sale ({@code use_yn='Y'})
 * items are exposed.
 *
 * @param id           goods item id
 * @param name         product name
 * @param price        selling price (KRW)
 * @param thumbnailUrl resolved thumbnail image URL (nullable)
 * @param soldOut      whether the item is flagged sold out
 */
public record GoodsListItem(
        Long id,
        String name,
        long price,
        String thumbnailUrl,
        boolean soldOut) {
}
