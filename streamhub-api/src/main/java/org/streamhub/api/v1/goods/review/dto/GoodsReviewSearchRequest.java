package org.streamhub.api.v1.goods.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Optional list filter for goods reviews. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsReviewSearchRequest {
    private String displayYn;
}
