package org.streamhub.api.v1.goods.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Answer body for a goods inquiry. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsInquiryAnswerRequest {

    @NotBlank
    private String answerContent;
}
