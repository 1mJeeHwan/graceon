package org.streamhub.api.v1.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.streamhub.api.v1.goods.entity.GoodsStatus;

/**
 * Create/update-goods payload. {@code thumbnailKey} and image {@code s3Key}s come from
 * prior /upload calls. {@code options}/{@code images} are dynamic rows (replace-on-save).
 */
public record GoodsCreateRequest(
        @NotNull(message = "분류는 필수입니다") Long categoryId,
        @NotBlank(message = "상품명을 입력하세요") String name,
        @NotBlank(message = "상품코드를 입력하세요") String code,
        String description,
        @NotNull(message = "판매가는 필수입니다") Long price,
        Long listPrice,
        Integer stock,
        Integer notiQty,
        String soldOut,
        String useYn,
        GoodsStatus status,
        String thumbnailKey,
        List<String> badges,
        List<GoodsOptionDto> options,
        List<GoodsImageDto> images) {
}
