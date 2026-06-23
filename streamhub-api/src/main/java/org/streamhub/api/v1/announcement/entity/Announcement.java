package org.streamhub.api.v1.announcement.entity;

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
import org.streamhub.api.v1.banner.entity.BannerLinkType;

/**
 * A modal-ad announcement (안내창) shown to users as an image popup, managed like a banner: each row
 * is a separate ad with its own image, link target, display window, and order. Display is gated by
 * {@code enabled} and the {@code startAt}/{@code endAt} window.
 *
 * <p>Reuses the {@link BannerLinkType} link model: VIDEO/MUSIC/POST pair with {@link #linkRefId} and
 * resolve to an internal path in the public response; {@code URL} (or a null type) uses
 * {@link #linkUrl} directly. The legacy {@code enabled}/{@code link_url}/{@code updated_at} columns
 * are reused; the new columns are added by Hibernate ({@code ddl-auto=update}) as nullable so the
 * pre-existing single-row config does not block the migration (the seeder then replaces it).
 */
@Entity
@Table(name = "ANNOUNCEMENT", indexes = {
        @Index(name = "idx_announcement_use", columnList = "enabled")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 200)
    private String title;

    /** Modal-ad image (CDN/proxy URL). Blank renders a text-only modal on the user site. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Structured link target. Null = uses {@link #linkUrl} directly. */
    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = 20)
    private BannerLinkType linkType;

    /** Referenced content/post id for VIDEO/MUSIC/POST link types. */
    @Column(name = "link_ref_id")
    private Long linkRefId;

    /** Title of the selected content captured at edit time, for admin-form display. */
    @Column(name = "link_label", length = 200)
    private String linkLabel;

    /** Raw URL for {@link BannerLinkType#URL} / legacy. Reuses the existing {@code link_url} column. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Visibility toggle. Reuses the existing {@code enabled} column. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Announcement(String title, String imageUrl, BannerLinkType linkType, Long linkRefId,
                         String linkLabel, String linkUrl, LocalDateTime startAt, LocalDateTime endAt,
                         int sortOrder, boolean enabled, LocalDateTime createdAt) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkType = linkType;
        this.linkRefId = linkRefId;
        this.linkLabel = linkLabel;
        this.linkUrl = linkUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** Updates editable fields. */
    public void update(String title, String imageUrl, BannerLinkType linkType, Long linkRefId,
                       String linkLabel, String linkUrl, LocalDateTime startAt, LocalDateTime endAt,
                       int sortOrder, boolean enabled) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkType = linkType;
        this.linkRefId = linkRefId;
        this.linkLabel = linkLabel;
        this.linkUrl = linkUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    /** Updates only the display order (drag-to-reorder). */
    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }
}
