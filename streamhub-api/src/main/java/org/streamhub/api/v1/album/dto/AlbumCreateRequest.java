package org.streamhub.api.v1.album.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.streamhub.api.v1.album.entity.AlbumGenre;
import org.streamhub.api.v1.album.entity.AlbumStatus;

/**
 * Create/update-album payload. {@code coverKey} comes from a prior /upload call.
 * {@code price}/{@code stock} feed the bridge {@code GOODS_ITEM} (commerce absorption);
 * {@code tracks} are dynamic rows (replace-on-save). Mirrors {@code GoodsCreateRequest}.
 */
public record AlbumCreateRequest(
        @NotBlank(message = "앨범명을 입력하세요") String title,
        @NotBlank(message = "아티스트를 입력하세요") String artist,
        String label,
        @NotNull(message = "장르는 필수입니다") AlbumGenre genre,
        LocalDate releaseDate,
        String description,
        String coverKey,
        AlbumStatus status,
        @NotNull(message = "판매가는 필수입니다") Long price,
        Integer stock,
        @Valid List<TrackDto> tracks) {
}
