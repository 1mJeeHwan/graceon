package org.streamhub.api.v1.album.hls;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.streamhub.api.v1.album.entity.Track;
import org.streamhub.api.v1.album.repository.TrackRepository;
import org.streamhub.api.base.storage.StorageService;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the encrypted-HLS serving logic: the playlist is rewritten so segments point at
 * the CDN and the key URI is the relative key endpoint. Music is free to listen, so the AES key is
 * served to anyone (no purchase gate).
 */
@ExtendWith(MockitoExtension.class)
class HlsStreamingServiceTest {

    private static final String CDN = "https://cdn.example.com";
    private static final String KEY_HEX = "000102030405060708090a0b0c0d0e0f";

    @Mock private TrackRepository trackRepository;
    @Mock private HlsKeyRepository hlsKeyRepository;
    @Mock private StorageService storageService;

    private HlsStreamingService service() {
        return new HlsStreamingService(trackRepository, hlsKeyRepository, storageService, CDN);
    }

    private Track packagedTrack() {
        Track track = Track.builder().albumId(5L).trackNo(1).title("새벽 찬양").build();
        ReflectionTestUtils.setField(track, "id", 7L);
        track.attachHls("hls/track-7/", 42L, 210);
        return track;
    }

    @Test
    void playlist_rewritesSegmentsToCdn_andKeyUriToRelative() {
        String ffmpegM3u8 = String.join("\n",
                "#EXTM3U",
                "#EXT-X-VERSION:3",
                "#EXT-X-KEY:METHOD=AES-128,URI=\"enc.key\",IV=0x0102",
                "#EXTINF:6.000,",
                "seg000.ts",
                "#EXTINF:4.500,",
                "seg001.ts",
                "#EXT-X-ENDLIST");
        when(trackRepository.findByIdAndAlbumId(7L, 5L)).thenReturn(Optional.of(packagedTrack()));
        when(storageService.getBytes("hls/track-7/index.m3u8"))
                .thenReturn(ffmpegM3u8.getBytes(StandardCharsets.UTF_8));

        String out = service().playlist(5L, 7L);

        assertThat(out).contains("URI=\"key\"").doesNotContain("enc.key");
        assertThat(out).contains(CDN + "/hls/track-7/seg000.ts");
        assertThat(out).contains(CDN + "/hls/track-7/seg001.ts");
        assertThat(out).contains("IV=0x0102"); // IV preserved
    }

    @Test
    void serveKey_returnsRawKeyBytes_forAnyone() {
        when(trackRepository.findByIdAndAlbumId(7L, 5L)).thenReturn(Optional.of(packagedTrack()));
        HlsKey key = HlsKey.builder().trackId(7L).keyHex(KEY_HEX).ivHex(KEY_HEX).build();
        when(hlsKeyRepository.findById(42L)).thenReturn(Optional.of(key));

        // No member id, no purchase — still served.
        byte[] result = service().serveKey(5L, 7L, null);

        assertThat(result).hasSize(16).isEqualTo(HexFormat.of().parseHex(KEY_HEX));
    }
}
