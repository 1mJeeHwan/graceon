package org.streamhub.api.v1.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.v1.media.dto.MediaAssetDto;

/**
 * Media library — a central registry of uploaded images. Upload returns the CDN URL (to embed in
 * banners / rich-text bodies); list/delete power the management page. SYSTEM/CHURCH_MANAGER write,
 * VIEWER read (via {@code media:read}).
 */
@Tag(name = "Media", description = "미디어 라이브러리 (업로드 이미지 관리)")
@RestController
@RequestMapping("/v1/media")
@PreAuthorize("hasAuthority('media:read')") // class default = read; mutations require media:write
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Operation(summary = "미디어 업로드", description = "파일을 CDN에 업로드하고 자산으로 기록한 뒤 key/url을 반환한다.")
    @PreAuthorize("hasAuthority('media:write')")
    @PostMapping("/upload")
    public ResultDTO<MediaAssetDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal AdminPrincipal principal) {
        Long uploadedBy = principal == null ? null : principal.id();
        return ResultDTO.ok(mediaService.upload(file, category, uploadedBy));
    }

    @Operation(summary = "미디어 목록", description = "카테고리/파일명 검색 + 페이지네이션된 업로드 이미지 목록.")
    @GetMapping
    public ResultDTO<ResInfinityList<MediaAssetDto>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize) {
        return ResultDTO.ok(mediaService.list(
                category, keyword,
                pageNumber == null ? 0 : pageNumber,
                pageSize == null ? 24 : pageSize));
    }

    @Operation(summary = "미디어 카테고리", description = "라이브러리 필터용 카테고리 목록.")
    @GetMapping("/categories")
    public ResultDTO<List<String>> categories() {
        return ResultDTO.ok(mediaService.categories());
    }

    @Operation(summary = "미디어 삭제", description = "자산 기록과 저장된 객체를 삭제한다.")
    @PreAuthorize("hasAuthority('media:write')")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        mediaService.delete(id);
        return ResultDTO.ok();
    }
}
