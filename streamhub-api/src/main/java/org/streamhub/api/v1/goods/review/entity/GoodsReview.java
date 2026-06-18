package org.streamhub.api.v1.goods.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A customer review of a goods item. All values are demo/fictional (PII guard).
 */
@Entity
@Table(name = "GOODS_REVIEW", indexes = {
        @Index(name = "idx_goods_review_item", columnList = "goods_item_id"),
        @Index(name = "idx_goods_review_display", columnList = "display_yn"),
        @Index(name = "idx_goods_review_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsReview {

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

    /** Star rating, 1–5. */
    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /** "Y"/"N" — shown on the storefront. */
    @Column(name = "display_yn", nullable = false, length = 1)
    private String displayYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private GoodsReview(Long goodsItemId, Long memberId, String memberName, int rating,
                        String content, String displayYn, LocalDateTime createdAt) {
        this.goodsItemId = goodsItemId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.rating = rating;
        this.content = content;
        this.displayYn = displayYn != null ? displayYn : "Y";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** Toggles storefront visibility. */
    public void changeDisplayYn(String displayYn) {
        this.displayYn = displayYn;
    }

    /** Updates the star rating. */
    public void changeRating(int rating) {
        this.rating = rating;
    }
}
