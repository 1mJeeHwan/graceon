package org.streamhub.api.v1.pub.me.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /pub/v1/me/inquiries} — the member asks a question about a goods item.
 *
 * @param goodsItemId the goods item being asked about
 * @param title       the inquiry title (≤200 chars)
 * @param content     the inquiry body (≤1000 chars)
 */
public record InquiryCreateRequest(
        @NotNull Long goodsItemId,
        @NotNull @Size(max = 200) String title,
        @NotNull @Size(max = 1000) String content) {
}
