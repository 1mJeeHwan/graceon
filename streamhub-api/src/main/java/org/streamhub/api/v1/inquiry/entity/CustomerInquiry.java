package org.streamhub.api.v1.inquiry.entity;

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
 * A 1:1 customer support inquiry (고객 문의). Distinct from any goods/product inquiry —
 * this is the general support queue an operator works through. {@code memberId} is
 * nullable (guest or anonymized inquiries keep only the masked {@code memberName}).
 * All values are demo/fictional (PII guard).
 */
@Entity
@Table(name = "CUSTOMER_INQUIRY", indexes = {
        @Index(name = "idx_customer_inquiry_status", columnList = "status"),
        @Index(name = "idx_customer_inquiry_category", columnList = "category"),
        @Index(name = "idx_customer_inquiry_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → MEMBER; null for guest/anonymized inquiries. */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private InquiryCategory category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "answer_content", length = 1000)
    private String answerContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    private CustomerInquiry(Long memberId, String memberName, InquiryCategory category,
                           String title, String content, InquiryStatus status,
                           String answerContent, LocalDateTime createdAt, LocalDateTime answeredAt) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = status != null ? status : InquiryStatus.OPEN;
        this.answerContent = answerContent;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.answeredAt = answeredAt;
    }

    /** Records an operator answer and flips the inquiry to {@link InquiryStatus#ANSWERED}. */
    public void answer(String answerContent) {
        this.answerContent = answerContent;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }

    /** Closes the inquiry; no further action expected. */
    public void close() {
        this.status = InquiryStatus.CLOSED;
    }
}
