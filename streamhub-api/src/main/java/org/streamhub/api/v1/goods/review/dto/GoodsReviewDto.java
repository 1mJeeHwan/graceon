package org.streamhub.api.v1.goods.review.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.review.entity.GoodsReview;

/**
 * A goods review row. Used as the list output. All values are demo/fictional (PII guard).
 * Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsReviewDto {
    private Long id;
    private Long goodsItemId;
    private Long memberId;
    private String memberName;
    private int rating;
    private String content;
    private String displayYn;
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted review. */
    public static GoodsReviewDto from(GoodsReview review) {
        GoodsReviewDto dto = new GoodsReviewDto();
        dto.id = review.getId();
        dto.goodsItemId = review.getGoodsItemId();
        dto.memberId = review.getMemberId();
        dto.memberName = review.getMemberName();
        dto.rating = review.getRating();
        dto.content = review.getContent();
        dto.displayYn = review.getDisplayYn();
        dto.createdAt = review.getCreatedAt();
        return dto;
    }
}
