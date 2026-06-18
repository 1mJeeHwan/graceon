package org.streamhub.api.v1.goods.inquiry.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.goods.inquiry.entity.AnswerStatus;
import org.streamhub.api.v1.goods.inquiry.entity.GoodsInquiry;

/**
 * A goods inquiry row. Used as the list/detail output. All values are demo/fictional
 * (PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class GoodsInquiryDto {
    private Long id;
    private Long goodsItemId;
    private Long memberId;
    private String memberName;
    private String title;
    private String content;
    private AnswerStatus answerStatus;
    private String answerContent;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    /** Builds a DTO from a persisted inquiry. */
    public static GoodsInquiryDto from(GoodsInquiry inquiry) {
        GoodsInquiryDto dto = new GoodsInquiryDto();
        dto.id = inquiry.getId();
        dto.goodsItemId = inquiry.getGoodsItemId();
        dto.memberId = inquiry.getMemberId();
        dto.memberName = inquiry.getMemberName();
        dto.title = inquiry.getTitle();
        dto.content = inquiry.getContent();
        dto.answerStatus = inquiry.getAnswerStatus();
        dto.answerContent = inquiry.getAnswerContent();
        dto.createdAt = inquiry.getCreatedAt();
        dto.answeredAt = inquiry.getAnsweredAt();
        return dto;
    }
}
