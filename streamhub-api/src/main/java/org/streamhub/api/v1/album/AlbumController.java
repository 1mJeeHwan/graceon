package org.streamhub.api.v1.album;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.album.dto.AlbumCreateRequest;
import org.streamhub.api.v1.album.dto.AlbumDetail;
import org.streamhub.api.v1.album.dto.AlbumListItem;
import org.streamhub.api.v1.album.dto.AlbumSearchRequest;
import org.streamhub.api.v1.goods.dto.UploadResponse;

/**
 * CCM album management endpoints (SYSTEM or CHURCH_MANAGER). Public album/preview
 * browsing lives in {@code PublicController}.
 */
@Tag(name = "Album", description = "CCM 음반 관리")
@RestController
@RequestMapping("/v1/album")
@PreAuthorize("hasAuthority('album:read')") // class default = read; mutations require album:write
public class AlbumController {

    private final AlbumService albumService;
    private final StorageService storageService;

    public AlbumController(AlbumService albumService, StorageService storageService) {
        this.albumService = albumService;
        this.storageService = storageService;
    }

    @Operation(summary = "앨범 목록", description = "검색/필터/페이지네이션된 음반 목록.")
    @PostMapping("/list")
    public ResultDTO<ResInfinityList<AlbumListItem>> list(@RequestBody AlbumSearchRequest request) {
        return ResultDTO.ok(albumService.list(request));
    }

    @Operation(summary = "앨범 상세", description = "트랙리스트 포함.")
    @GetMapping("/{id}")
    public ResultDTO<AlbumDetail> detail(@PathVariable Long id) {
        return ResultDTO.ok(albumService.getDetail(id));
    }

    @Operation(summary = "앨범 등록", description = "판매 활성 시 브리지 GOODS_ITEM을 자동 생성한다.")
    @PreAuthorize("hasAuthority('album:write')")
    @PostMapping
    public ResultDTO<AlbumDetail> create(@Valid @RequestBody AlbumCreateRequest request) {
        return ResultDTO.ok(albumService.create(request));
    }

    @Operation(summary = "앨범 수정", description = "트랙/브리지 GOODS_ITEM을 동기화한다.")
    @PreAuthorize("hasAuthority('album:write')")
    @PutMapping("/{id}")
    public ResultDTO<AlbumDetail> update(
            @PathVariable Long id, @Valid @RequestBody AlbumCreateRequest request) {
        return ResultDTO.ok(albumService.update(id, request));
    }

    @Operation(summary = "앨범 삭제", description = "트랙은 삭제, 브리지 GOODS_ITEM은 미판매 전환(주문이력 보호).")
    @PreAuthorize("hasAuthority('album:write')")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        albumService.delete(id);
        return ResultDTO.ok();
    }

    @Operation(summary = "커버 이미지 업로드", description = "앨범 커버를 스토리지에 업로드하고 key/url을 반환한다.")
    @PreAuthorize("hasAuthority('album:write')")
    @PostMapping("/upload")
    public ResultDTO<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String key = storageService.upload(file, "album");
        return ResultDTO.ok(new UploadResponse(key, storageService.publicUrl(key)));
    }
}
