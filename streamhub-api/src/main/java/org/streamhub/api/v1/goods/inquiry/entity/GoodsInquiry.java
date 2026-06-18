package org.streamhub.api.v1.goods.inquiry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A customer inquiry about a goods item (Q&A). All values are demo/fictional (PII guard).
 */
@Entity
@Table(name = "GOODS_INQUIRY", indexes = {
        @Index(name = "idx_goods_inquiry_item", columnList = "goods_item_id"),
        @Index(name = "idx_goods_inquiry_status", columnList = "answer_status"),
        @Index(name = "idx_goods_inquiry_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → GOODS_ITEM. */
    @Column(name = "goods_item_id", nullable = false)
    private Long goodsItemId;

    /** FK → MEMBER (nullable for guest/withdrawn members). */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status", nullable = false, length = 10)
    private AnswerStatus answerStatus;

    @Column(name = "answer_content", length = 1000)
    private String answerContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    private GoodsInquiry(Long goodsItemId, Long memberId, String memberName, String title,
                         String content, AnswerStatus answerStatus, String answerContent,
                         LocalDateTime createdAt, LocalDateTime answeredAt) {
        this.goodsItemId = goodsItemId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.title = title;
        this.content = content;
        this.answerStatus = answerStatus != null ? answerStatus : AnswerStatus.WAITING;
        this.answerContent = answerContent;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.answeredAt = answeredAt;
    }

    /** Records an answer and flips the status to {@link AnswerStatus#ANSWERED}. */
    public void answer(String answerContent) {
        this.answerContent = answerContent;
        this.answerStatus = AnswerStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }
}
