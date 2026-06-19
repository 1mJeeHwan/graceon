package org.streamhub.api.v1.album.hls;

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
 * The AES-128 key + IV for a track's encrypted HLS stream. This is the single secret that gates
 * playback: the encrypted {@code .ts} segments are public/CDN-cacheable, but they are useless
 * without this 16-byte key, which is served only to a member who purchased the album
 * (see {@code HlsStreamingService#serveKey}).
 *
 * <p>Stored as hex. The key never appears in the playlist, in S3, or in any client response except
 * the access-gated {@code /hls/key} endpoint.
 */
@Entity
@Table(name = "TRACK_HLS_KEY", indexes = {
        @Index(name = "idx_hls_key_track", columnList = "track_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HlsKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    /** 16-byte AES-128 key, hex-encoded (32 chars). */
    @Column(name = "key_hex", nullable = false, length = 32)
    private String keyHex;

    /** 16-byte initialization vector, hex-encoded (32 chars). */
    @Column(name = "iv_hex", nullable = false, length = 32)
    private String ivHex;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private HlsKey(Long trackId, String keyHex, String ivHex, LocalDateTime createdAt) {
        this.trackId = trackId;
        this.keyHex = keyHex;
        this.ivHex = ivHex;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
