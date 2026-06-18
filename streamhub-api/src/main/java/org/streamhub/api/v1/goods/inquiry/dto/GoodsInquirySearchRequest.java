package org.streamhub.api.v1.goods.inquiry.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.inquiry.entity.AnswerStatus;

/** Optional list filter for goods inquiries. */
@Getter
@Setter
@NoArgsConstructor
public class GoodsInquirySearchRequest {
    private AnswerStatus answerStatus;
}
