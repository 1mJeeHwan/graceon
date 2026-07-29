package org.streamhub.api.v1.media;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.media.dto.MediaAssetDto;
import org.streamhub.api.v1.media.entity.MediaAsset;
import org.streamhub.api.v1.media.repository.MediaAssetRepository;

/**
 * Media library: records every admin upload as a {@link MediaAsset} so images can be browsed,
 * reused (their CDN URL copied into banners / rich-text bodies), and deleted from one place.
 */
@Service
public class MediaService {

    private static final int DEFAULT_PAGE_SIZE = 24;

    /** Category fallback when the caller sends nothing usable. */
    private static final String DEFAULT_CATEGORY = "general";

    /** Lowercase slug — letters, digits, hyphen, underscore. No dots, no path separators. */
    private static final Pattern SAFE_CATEGORY = Pattern.compile("[a-z0-9_-]{1,40}");

    private final MediaAssetRepository mediaAssetRepository;
    private final StorageService storageService;

    public MediaService(MediaAssetRepository mediaAssetRepository, StorageService storageService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.storageService = storageService;
    }

    /** Stores the file under {@code media/<category>}, records the asset, and returns its CDN URL. */
    @Transactional
    public MediaAssetDto upload(MultipartFile file, String category, Long uploadedBy) {
        String safeCategory = blankToDefault(category);
        String key = storageService.upload(file, "media/" + safeCategory);
        MediaAsset asset = mediaAssetRepository.save(MediaAsset.builder()
                .storageKey(key)
                .category(safeCategory)
                .originalName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedBy(uploadedBy)
                .build());
        return toDto(asset);
    }

    @Transactional(readOnly = true)
    public ResInfinityList<MediaAssetDto> list(String category, String keyword, int pageNumber, int pageSize) {
        int size = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        int page = Math.max(pageNumber, 0);
        Page<MediaAsset> result = mediaAssetRepository.search(
                blankToNull(category), blankToNull(keyword),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        List<MediaAssetDto> items = result.getContent().stream().map(this::toDto).toList();
        return ResInfinityList.of(items, result.getTotalElements(), size);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return mediaAssetRepository.findDistinctCategories();
    }

    /** Deletes the asset record and its stored object (skipped for seeded external URLs). */
    @Transactional
    public void delete(Long id) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        String key = asset.getStorageKey();
        if (key != null && !key.startsWith("http://") && !key.startsWith("https://")) {
            storageService.delete(key);
        }
        mediaAssetRepository.delete(asset);
    }

    private MediaAssetDto toDto(MediaAsset asset) {
        return MediaAssetDto.of(asset, storageService.publicUrl(asset.getStorageKey()));
    }

    /**
     * Normalizes the caller-supplied category into a safe key segment.
     *
     * <p>The value arrives as an unvalidated request parameter and becomes part of the S3 object
     * key ({@code media/<category>/...}). Trimming alone let {@code ../} and {@code /} through,
     * which lets an uploader place objects outside the {@code media/} namespace the read path
     * whitelists. Anything that is not a simple slug falls back to {@code general} rather than
     * failing the upload — the category is a filing label, not a security decision, and the read
     * side ({@code MediaPublicController}) enforces the prefix whitelist independently.
     */
    private String blankToDefault(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CATEGORY;
        }
        String trimmed = value.trim().toLowerCase();
        return SAFE_CATEGORY.matcher(trimmed).matches() ? trimmed : DEFAULT_CATEGORY;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
