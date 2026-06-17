package org.streamhub.api.v1.goods.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * AG Grid inline bulk-update payload. Each {@link Row} carries only the changed cells;
 * {@code null} fields keep the existing value.
 */
public record GoodsBulkUpdateRequest(@NotEmpty(message = "수정할 행이 없습니다") List<Row> rows) {

    public record Row(
            @NotNull Long id,
            Integer stock,
            Integer notiQty,
            Long price,
            String soldOut,
            String useYn,
            Integer sort) {
    }
}
