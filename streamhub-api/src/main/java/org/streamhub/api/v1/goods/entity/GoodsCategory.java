package org.streamhub.api.v1.goods.entity;

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

/** A goods category node in the 3-level shop tree (root has {@code parentId == null}). */
@Entity
@Table(name = "GOODS_CATEGORY", indexes = {
        @Index(name = "idx_goods_category_parent", columnList = "parent_id"),
        @Index(name = "idx_goods_category_sort", columnList = "sort")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent category id; {@code null} for root nodes. */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Tree depth, 1..3. */
    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    /** Category representative image storage key. */
    @Column(name = "image_key", length = 300)
    private String imageKey;

    /** "Y"/"N" — whether the category is sellable. */
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private GoodsCategory(Long parentId, String name, Integer depth, Integer sort,
                          String imageKey, String useYn, LocalDateTime createdAt) {
        this.parentId = parentId;
        this.name = name;
        this.depth = depth;
        this.sort = sort != null ? sort : 0;
        this.imageKey = imageKey;
        this.useYn = useYn;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Updates editable fields. */
    public void update(Long parentId, String name, Integer depth, Integer sort,
                       String imageKey, String useYn) {
        this.parentId = parentId;
        this.name = name;
        this.depth = depth;
        this.sort = sort;
        this.imageKey = imageKey;
        this.useYn = useYn;
        this.updatedAt = LocalDateTime.now();
    }
}
