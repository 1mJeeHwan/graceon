package org.streamhub.api.v1.goods.stock.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stock-quantity update input. {@code stock} is required; {@code notiQty} is optional and,
 * when omitted, the existing notify threshold is kept.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsStockUpdateRequest {
    private Integer stock;
    private Integer notiQty;
}
