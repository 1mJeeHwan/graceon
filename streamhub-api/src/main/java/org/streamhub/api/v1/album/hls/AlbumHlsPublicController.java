package org.streamhub.api.v1.album.hls;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for encrypted full-track HLS playback (member site).
 *
 * <ul>
 *   <li>{@code GET .../hls/index.m3u8} — the playlist.</li>
 *   <li>{@code GET .../hls/key} — the AES-128 key.</li>
 * </ul>
 *
 * <p>Both are fully public: music is a free listening experience, so neither endpoint authenticates
 * the caller. Earlier revisions gated the key on album purchase and this class still parsed a member
 * Bearer token to pass along; that parameter was dead once the paid tier was removed, so it is gone.
 * A comment promising a gate that the service does not apply is worse than no comment at all.
 *
 * The encrypted {@code .ts} segments themselves are served by the CDN (CloudFront → S3), not here.
 */
@Tag(name = "Album HLS", description = "암호화 풀트랙 스트리밍 (HLS + AES-128)")
@RestController
@RequestMapping("/pub/v1/albums/{albumId}/tracks/{trackId}/hls")
public class AlbumHlsPublicController {

    private static final String HLS_MIME = "application/vnd.apple.mpegurl";

    private final HlsStreamingService hlsStreamingService;

    public AlbumHlsPublicController(HlsStreamingService hlsStreamingService) {
        this.hlsStreamingService = hlsStreamingService;
    }

    @Operation(summary = "HLS 플레이리스트", description = "세그먼트 URL은 CDN, 키 URI는 게이트된 키 엔드포인트로 재작성됨. 공개.")
    @GetMapping(value = "/index.m3u8", produces = HLS_MIME)
    public ResponseEntity<String> playlist(@PathVariable Long albumId, @PathVariable Long trackId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(HLS_MIME))
                .cacheControl(CacheControl.noCache())
                .body(hlsStreamingService.playlist(albumId, trackId));
    }

    @Operation(summary = "AES-128 키",
            description = "16바이트 AES 키를 반환한다. 음악은 무료 감상이라 구매/로그인 게이트가 없다(공개). 캐시 금지.")
    @GetMapping(value = "/key", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> key(@PathVariable Long albumId, @PathVariable Long trackId) {
        byte[] keyBytes = hlsStreamingService.serveKey(albumId, trackId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.noStore())
                .body(keyBytes);
    }
}
