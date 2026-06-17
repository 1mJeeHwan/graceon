package org.streamhub.api.v1.goods.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** An additional gallery image of a {@link GoodsItem}. */
@Entity
@Table(name = "GOODS_IMAGE", indexes = {
        @Index(name = "idx_goods_image_item", columnList = "item_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → GOODS_ITEM. */
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Storage key (same column name as the ContentFile convention). */
    @Column(name = "s3_key", nullable = false, length = 300)
    private String s3Key;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Builder
    private GoodsImage(Long itemId, String s3Key, Integer sort) {
        this.itemId = itemId;
        this.s3Key = s3Key;
        this.sort = sort != null ? sort : 0;
    }
}
