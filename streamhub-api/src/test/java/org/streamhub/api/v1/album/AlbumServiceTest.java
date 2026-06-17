package org.streamhub.api.v1.album;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.album.dto.PreviewResponse;
import org.streamhub.api.v1.album.entity.Track;
import org.streamhub.api.v1.album.provider.MusicPreviewProvider;
import org.streamhub.api.v1.album.repository.TrackRepository;

/**
 * Unit tests for {@link AlbumService#getPreview} null-safety: when the preview provider yields no
 * playable URL (null/blank — e.g. a custom provider with no source for the track), the service
 * returns {@code NOT_FOUND} instead of shipping a broken URL to the mini-player.
 */
@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private TrackRepository trackRepository;
    @Mock
    private MusicPreviewProvider musicPreviewProvider;

    @InjectMocks
    private AlbumService albumService;

    private Track track() {
        return Track.builder()
                .albumId(1L).trackNo(3).title("3번 트랙")
                .previewStartSec(0).previewLengthSec(30).build();
    }

    @Test
    void getPreview_blankResolvedUrl_isNotFound() {
        when(trackRepository.findByIdAndAlbumId(5L, 1L)).thenReturn(Optional.of(track()));
        when(musicPreviewProvider.resolvePreviewUrl(eq(1L), anyInt(), any())).thenReturn("   ");

        assertThatThrownBy(() -> albumService.getPreview(1L, 5L))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void getPreview_playableUrl_returnsPreview() {
        when(trackRepository.findByIdAndAlbumId(5L, 1L)).thenReturn(Optional.of(track()));
        when(musicPreviewProvider.resolvePreviewUrl(eq(1L), anyInt(), any()))
                .thenReturn("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
        when(musicPreviewProvider.isDemo()).thenReturn(true);

        PreviewResponse response = albumService.getPreview(1L, 5L);

        assertThat(response.previewUrl()).isEqualTo(
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
        assertThat(response.lengthSec()).isEqualTo(30);
        assertThat(response.demo()).isTrue();
    }
}
