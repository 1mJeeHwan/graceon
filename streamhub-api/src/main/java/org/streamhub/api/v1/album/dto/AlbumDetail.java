package org.streamhub.api.v1.album.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.album.entity.AlbumGenre;
import org.streamhub.api.v1.album.entity.AlbumStatus;
import org.streamhub.api.v1.album.entity.MusicSource;

/** Full album detail. Base fields from MyBatis; coverUrl/tracks filled by the service. */
@Getter
@Setter
@NoArgsConstructor
public class AlbumDetail {
    private Long id;
    private Long goodsItemId;
    private String title;
    private String artist;
    private String label;
    private AlbumGenre genre;
    private AlbumStatus status;
    private LocalDate releaseDate;
    private String description;
    private String coverKey;
    private String coverUrl; // filled by the service from coverKey
    private Integer trackCount;
    private Long viewCount;
    private MusicSource source;
    private Long price; // filled by the service from the bridge GOODS_ITEM
    private Integer stock; // filled by the service from the bridge GOODS_ITEM
    private List<TrackDto> tracks; // filled by the service
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
