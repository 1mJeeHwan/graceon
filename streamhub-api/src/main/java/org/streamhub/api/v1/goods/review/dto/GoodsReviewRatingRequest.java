package org.streamhub.api.v1.goods.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rating-update body for a goods review. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsReviewRatingRequest {

    @Min(1)
    @Max(5)
    private int rating;
}
