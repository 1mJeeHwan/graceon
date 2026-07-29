package org.streamhub.api.v1.album.hls;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.album.entity.Track;
import org.streamhub.api.v1.album.repository.TrackRepository;

/**
 * Serves the encrypted full-track HLS stream.
 *
 * <ul>
 *   <li>{@link #playlist} (public) — returns {@code index.m3u8} rewritten so segment URLs point at
 *       the CDN ({@code segment-base-url}) and the AES key URI is a relative {@code key} (resolved
 *       by the player against the playlist URL → the key endpoint).</li>
 *   <li>{@link #serveKey} (public) — returns the raw 16-byte AES key to any caller. Music is free
 *       to listen, so there is no purchase gate anywhere in this flow.</li>
 * </ul>
 */
@Service
public class HlsStreamingService {

    private static final HexFormat HEX = HexFormat.of();

    private final TrackRepository trackRepository;
    private final HlsKeyRepository hlsKeyRepository;
    private final StorageService storageService;
    private final String segmentBaseUrl;

    public HlsStreamingService(TrackRepository trackRepository,
                               HlsKeyRepository hlsKeyRepository,
                               StorageService storageService,
                               @Value("${app.hls.segment-base-url:}") String segmentBaseUrl) {
        this.trackRepository = trackRepository;
        this.hlsKeyRepository = hlsKeyRepository;
        this.storageService = storageService;
        this.segmentBaseUrl = stripTrailingSlash(segmentBaseUrl);
    }

    /** Public: the rewritten HLS playlist (segments → CDN, key URI → relative gated endpoint). */
    @Transactional(readOnly = true)
    public String playlist(Long albumId, Long trackId) {
        Track track = requireFullTrack(albumId, trackId);
        String stored = new String(storageService.getBytes(track.getHlsPrefix() + "index.m3u8"),
                StandardCharsets.UTF_8);
        return rewrite(stored, track.getHlsPrefix());
    }

    /**
     * The raw AES-128 key for a full track, served to anyone.
     *
     * <p>Music is a free listening experience, so there is no purchase gate. The method therefore
     * takes no caller identity: an unused {@code memberId} parameter would read as a gate that
     * exists, and the next person to touch this code would trust a check that was never made.
     * The stream stays AES-packaged, but with the key public that is transport hygiene (it stops
     * casual segment scraping), <b>not</b> access control — restoring a paid tier means adding a
     * purchase lookup here plus short-lived signed key URLs.
     */
    @Transactional(readOnly = true)
    public byte[] serveKey(Long albumId, Long trackId) {
        Track track = requireFullTrack(albumId, trackId);
        HlsKey key = hlsKeyRepository.findById(track.getHlsKeyId())
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND, "키를 찾을 수 없습니다"));
        return HEX.parseHex(key.getKeyHex());
    }

    // --- helpers -----------------------------------------------------------

    private Track requireFullTrack(Long albumId, Long trackId) {
        Track track = trackRepository.findByIdAndAlbumId(trackId, albumId)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND, "트랙을 찾을 수 없습니다"));
        if (!track.isHasFullTrack() || track.getHlsPrefix() == null) {
            throw new ApiException(ResultCode.NOT_FOUND, "암호화 풀트랙이 없는 트랙입니다");
        }
        return track;
    }

    /**
     * Rewrites the stored ffmpeg playlist: each segment file → an absolute CDN URL, and the
     * AES key URI → the relative {@code key} endpoint (resolved against the playlist URL).
     */
    private String rewrite(String m3u8, String prefix) {
        StringBuilder out = new StringBuilder(m3u8.length() + 256);
        for (String line : m3u8.split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("#EXT-X-KEY")) {
                out.append(trimmed.replaceAll("URI=\"[^\"]*\"", "URI=\"key\"")).append('\n');
            } else if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.endsWith(".ts")) {
                out.append(segmentBaseUrl).append('/').append(prefix).append(trimmed).append('\n');
            } else if (!trimmed.isEmpty() || line.isEmpty()) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
