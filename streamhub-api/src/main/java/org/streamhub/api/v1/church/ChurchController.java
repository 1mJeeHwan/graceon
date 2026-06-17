package org.streamhub.api.v1.church;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.streamhub.api.v1.church.dto.ChurchDetail;
import org.streamhub.api.v1.church.dto.ChurchListItem;
import org.streamhub.api.v1.church.dto.ChurchSearchRequest;
import org.streamhub.api.v1.church.dto.ChurchUpsertRequest;
import org.streamhub.api.v1.church.dto.CodeLabel;
import org.streamhub.api.v1.content.dto.UploadResponse;

/**
 * Church management endpoints (SYSTEM or CHURCH_MANAGER). Location-based public search lives
 * on {@link org.streamhub.api.v1.pub.PublicController}.
 */
@Tag(name = "Church", description = "교회찾기 — 교회 관리")
@RestController
@RequestMapping("/v1/churches")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class ChurchController {

    private final ChurchService churchService;
    private final StorageService storageService;

    public ChurchController(ChurchService churchService, StorageService storageService) {
        this.churchService = churchService;
        this.storageService = storageService;
    }

    @Operation(summary = "교회 목록", description = "검색/필터/페이지네이션된 교회 목록.")
    @PostMapping("/list")
    public ResultDTO<ResInfinityList<ChurchListItem>> list(@RequestBody ChurchSearchRequest request) {
        return ResultDTO.ok(churchService.list(request));
    }

    @Operation(summary = "교회 상세", description = "예배시간 포함.")
    @GetMapping("/{id}")
    public ResultDTO<ChurchDetail> detail(@PathVariable Long id) {
        return ResultDTO.ok(churchService.getDetail(id));
    }

    @Operation(summary = "교회 등록")
    @PostMapping
    public ResultDTO<ChurchDetail> create(@Valid @RequestBody ChurchUpsertRequest request) {
        return ResultDTO.ok(churchService.create(request));
    }

    @Operation(summary = "교회 수정")
    @PutMapping("/{id}")
    public ResultDTO<ChurchDetail> update(
            @PathVariable Long id, @Valid @RequestBody ChurchUpsertRequest request) {
        return ResultDTO.ok(churchService.update(id, request));
    }

    @Operation(summary = "교회 삭제")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        churchService.delete(id);
        return ResultDTO.ok();
    }

    @Operation(summary = "교단 코드/라벨", description = "교회 등록 폼의 교단 선택용.")
    @GetMapping("/denominations")
    public ResultDTO<List<CodeLabel>> denominations() {
        return ResultDTO.ok(churchService.listDenominations());
    }

    @Operation(summary = "교회 이미지 업로드", description = "썸네일을 스토리지에 업로드하고 key/url을 반환한다.")
    @PostMapping("/upload")
    public ResultDTO<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String key = storageService.upload(file, "church");
        return ResultDTO.ok(new UploadResponse(key, storageService.publicUrl(key)));
    }
}
