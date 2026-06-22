package org.streamhub.api.v1.media.entity;

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
 * A first-class record of an uploaded media file, so images uploaded across the admin (banners,
 * rich-text bodies, etc.) can be browsed, reused, and cleaned up from one media library instead of
 * being scattered as ad-hoc key columns on each domain entity.
 *
 * <p>{@code storageKey} is the storage object key; its public URL is resolved on read through
 * {@code StorageService.publicUrl} (which passes absolute URLs through unchanged, so seeded
 * external sample images work too).
 */
@Entity
@Table(name = "MEDIA_ASSET", indexes = {
        @Index(name = "idx_media_category", columnList = "category"),
        @Index(name = "idx_media_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    /** Logical grouping (e.g. {@code banner}, {@code post}, {@code campaign}, {@code general}). */
    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Admin account id that uploaded the asset (null for seeded samples). */
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private MediaAsset(String storageKey, String category, String originalName,
                       String contentType, Long sizeBytes, Long uploadedBy) {
        this.storageKey = storageKey;
        this.category = category;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
        this.createdAt = LocalDateTime.now();
    }
}
