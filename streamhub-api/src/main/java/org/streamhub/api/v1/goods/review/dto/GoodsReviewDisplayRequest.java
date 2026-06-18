package org.streamhub.api.v1.goods.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Display-toggle body for a goods review. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsReviewDisplayRequest {

    @NotBlank
    private String displayYn;
}
