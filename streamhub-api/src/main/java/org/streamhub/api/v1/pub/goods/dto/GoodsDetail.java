package org.streamhub.api.v1.pub.goods.dto;

import java.util.List;

/**
 * Public goods (merchandise) storefront detail. Exposed only for on-sale ({@code use_yn='Y'}) items.
 * {@code imageUrls} are the resolved gallery image URLs in display order.
 *
 * @param id          goods item id
 * @param name        product name
 * @param price       selling price (KRW)
 * @param stock       remaining warehouse stock
 * @param soldOut     whether the item is flagged sold out
 * @param description product description (nullable)
 * @param imageUrls   resolved gallery image URLs (display order, may be empty)
 */
public record GoodsDetail(
        Long id,
        String name,
        long price,
        Integer stock,
        boolean soldOut,
        String description,
        List<String> imageUrls) {
}
