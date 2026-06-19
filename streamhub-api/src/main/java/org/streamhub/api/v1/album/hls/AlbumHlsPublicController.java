package org.streamhub.api.v1.album.hls;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.jwt.JwtTokenProvider;

/**
 * Public endpoints for encrypted full-track HLS playback (member site).
 *
 * <ul>
 *   <li>{@code GET .../hls/index.m3u8} — public playlist (the gate is the key, not the playlist).</li>
 *   <li>{@code GET .../hls/key} — the AES-128 key, returned only to a member who purchased the
 *       album. The browser player (hls.js) sends the member Bearer token on this request.</li>
 * </ul>
 *
 * The encrypted {@code .ts} segments themselves are served by the CDN (CloudFront → S3), not here.
 */
@Tag(name = "Album HLS", description = "암호화 풀트랙 스트리밍 (HLS + AES-128)")
@RestController
@RequestMapping("/pub/v1/albums/{albumId}/tracks/{trackId}/hls")
public class AlbumHlsPublicController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HLS_MIME = "application/vnd.apple.mpegurl";

    private final HlsStreamingService hlsStreamingService;
    private final JwtTokenProvider tokenProvider;

    public AlbumHlsPublicController(HlsStreamingService hlsStreamingService,
                                    JwtTokenProvider tokenProvider) {
        this.hlsStreamingService = hlsStreamingService;
        this.tokenProvider = tokenProvider;
    }

    @Operation(summary = "HLS 플레이리스트", description = "세그먼트 URL은 CDN, 키 URI는 게이트된 키 엔드포인트로 재작성됨. 공개.")
    @GetMapping(value = "/index.m3u8", produces = HLS_MIME)
    public ResponseEntity<String> playlist(@PathVariable Long albumId, @PathVariable Long trackId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(HLS_MIME))
                .cacheControl(CacheControl.noCache())
                .body(hlsStreamingService.playlist(albumId, trackId));
    }

    @Operation(summary = "AES-128 키", description = "해당 앨범을 구매한 회원에게만 16바이트 키를 반환. 캐시 금지.")
    @GetMapping(value = "/key", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> key(
            @PathVariable Long albumId,
            @PathVariable Long trackId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        byte[] keyBytes = hlsStreamingService.serveKey(albumId, trackId, resolveMemberId(authorization));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.noStore())
                .body(keyBytes);
    }

    /** Resolves the member id from a Bearer member token, or null if absent/invalid. */
    private Long resolveMemberId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            DecodedJWT jwt = tokenProvider.verify(authorization.substring(BEARER_PREFIX.length()));
            if (!tokenProvider.isMemberToken(jwt)) {
                return null;
            }
            return Long.valueOf(jwt.getSubject());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
