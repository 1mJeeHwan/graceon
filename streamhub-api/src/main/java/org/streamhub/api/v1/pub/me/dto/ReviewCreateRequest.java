package org.streamhub.api.v1.pub.me.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /pub/v1/me/reviews} — the member writes a review of a goods item.
 *
 * @param goodsItemId the goods item being reviewed
 * @param rating      star rating, 1–5
 * @param content     the review body (≤1000 chars)
 */
public record ReviewCreateRequest(
        @NotNull Long goodsItemId,
        @Min(1) @Max(5) int rating,
        @NotNull @Size(max = 1000) String content) {
}
