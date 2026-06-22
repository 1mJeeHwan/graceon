package org.streamhub.api.v1.media.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.streamhub.api.v1.media.entity.MediaAsset;

/** One media-library row. {@code url} is resolved from the storage key by the service. */
@Getter
@Builder
public class MediaAssetDto {
    private final Long id;
    private final String key;
    private final String url;
    private final String category;
    private final String originalName;
    private final String contentType;
    private final Long sizeBytes;
    private final LocalDateTime createdAt;

    public static MediaAssetDto of(MediaAsset asset, String url) {
        return MediaAssetDto.builder()
                .id(asset.getId())
                .key(asset.getStorageKey())
                .url(url)
                .category(asset.getCategory())
                .originalName(asset.getOriginalName())
                .contentType(asset.getContentType())
                .sizeBytes(asset.getSizeBytes())
                .createdAt(asset.getCreatedAt())
                .build();
    }
}
