package org.streamhub.api.v1.album.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A CCM album (C3). Owns a 1:N set of {@link Track} rows. Commerce (purchase/stock/
 * shipping/receipt) is absorbed via a 1:1 bridge to {@code GOODS_ITEM}
 * ({@code goodsItemId}) so the order domain is never modified.
 */
@Entity
@Table(name = "ALBUM", indexes = {
        @Index(name = "idx_album_status", columnList = "status"),
        @Index(name = "idx_album_genre", columnList = "genre"),
        @Index(name = "idx_album_goods", columnList = "goods_item_id"),
        @Index(name = "idx_album_release", columnList = "release_date"),
        @Index(name = "idx_album_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → GOODS_ITEM (sale bridge). Null = not for sale (intro only). */
    @Column(name = "goods_item_id")
    private Long goodsItemId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist", nullable = false, length = 120)
    private String artist;

    @Column(name = "label", length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 16)
    private AlbumGenre genre;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "cover_key", length = 300)
    private String coverKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AlbumStatus status;

    /** Track count (synced by the service). */
    @Column(name = "track_count", nullable = false)
    private Integer trackCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    /** Preview source (C3 music seam). */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 12)
    private MusicSource source;

    /** External album id when linked to a real music provider (currently null). */
    @Column(name = "external_id", length = 80)
    private String externalId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Album(Long goodsItemId, String title, String artist, String label, AlbumGenre genre,
                  LocalDate releaseDate, String description, String coverKey, AlbumStatus status,
                  Integer trackCount, Long viewCount, MusicSource source, String externalId,
                  LocalDateTime createdAt) {
        this.goodsItemId = goodsItemId;
        this.title = title;
        this.artist = artist;
        this.label = label;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.description = description;
        this.coverKey = coverKey;
        this.status = status != null ? status : AlbumStatus.ON_SALE;
        this.trackCount = trackCount != null ? trackCount : 0;
        this.viewCount = viewCount != null ? viewCount : 0L;
        this.source = source != null ? source : MusicSource.SEED;
        this.externalId = externalId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Updates editable metadata. */
    public void update(String title, String artist, String label, AlbumGenre genre,
                       LocalDate releaseDate, String description, String coverKey, AlbumStatus status) {
        this.title = title;
        this.artist = artist;
        this.label = label;
        this.genre = genre;
        this.releaseDate = releaseDate;
        this.description = description;
        this.coverKey = coverKey;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    /** Links the sale-bridge GOODS_ITEM. */
    public void linkGoodsItem(Long goodsItemId) {
        this.goodsItemId = goodsItemId;
        this.updatedAt = LocalDateTime.now();
    }

    /** Syncs the track count after a track replace. */
    public void syncTrackCount(int trackCount) {
        this.trackCount = trackCount;
        this.updatedAt = LocalDateTime.now();
    }

    /** Increments the view count. */
    public void increaseViewCount() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0L) + 1L;
    }
}
