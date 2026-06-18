package org.streamhub.api.v1.goods.stock.dto;

/**
 * Stock list filter. All fields optional: {@code keyword} matches code or name,
 * {@code lowStock="Y"} keeps only items at or below their notify threshold, and
 * {@code sortByStockAsc=true} orders by stock ascending instead of newest-first.
 *
 * @param keyword        optional code/name substring filter
 * @param lowStock       "Y" to keep only low-stock items ({@code stock <= notiQty})
 * @param sortByStockAsc when true, sort by stock ascending (else newest first)
 */
public record GoodsStockSearchRequest(String keyword, String lowStock, Boolean sortByStockAsc) {
}
