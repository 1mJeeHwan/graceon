package org.streamhub.api.v1.inquiry.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.inquiry.entity.CustomerInquiry;
import org.streamhub.api.v1.inquiry.entity.InquiryCategory;
import org.streamhub.api.v1.inquiry.entity.InquiryStatus;

/**
 * A 1:1 customer inquiry row, used as the list and detail output. All values are
 * demo/fictional (PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class InquiryDto {
    private Long id;
    private Long memberId;
    private String memberName;
    private InquiryCategory category;
    private String title;
    private String content;
    private InquiryStatus status;
    private String answerContent;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    /** Builds a DTO from a persisted inquiry. */
    public static InquiryDto from(CustomerInquiry inquiry) {
        InquiryDto dto = new InquiryDto();
        dto.id = inquiry.getId();
        dto.memberId = inquiry.getMemberId();
        dto.memberName = inquiry.getMemberName();
        dto.category = inquiry.getCategory();
        dto.title = inquiry.getTitle();
        dto.content = inquiry.getContent();
        dto.status = inquiry.getStatus();
        dto.answerContent = inquiry.getAnswerContent();
        dto.createdAt = inquiry.getCreatedAt();
        dto.answeredAt = inquiry.getAnsweredAt();
        return dto;
    }
}
