package org.streamhub.api.v1.banner;

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
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.banner.dto.BannerDto;
import org.streamhub.api.v1.banner.dto.BannerSearchRequest;
import org.streamhub.api.v1.banner.dto.BannerSortRequest;

/**
 * Banner management endpoints (SYSTEM or CHURCH_MANAGER). Banners drive the front main slots,
 * side rail, and popup.
 */
@Tag(name = "Banner", description = "프론트 배너 관리")
@RestController
@RequestMapping("/v1/banner")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @Operation(summary = "배너 목록", description = "노출위치/디바이스/사용여부 필터, 정렬순서 오름차순 목록.")
    @PostMapping("/list")
    public ResultDTO<List<BannerDto>> list(@RequestBody BannerSearchRequest request) {
        return ResultDTO.ok(bannerService.list(request));
    }

    @Operation(summary = "배너 상세")
    @GetMapping("/{id}")
    public ResultDTO<BannerDto> detail(@PathVariable Long id) {
        return ResultDTO.ok(bannerService.getDetail(id));
    }

    @Operation(summary = "배너 등록")
    @PostMapping
    public ResultDTO<BannerDto> create(@Valid @RequestBody BannerDto request) {
        return ResultDTO.ok(bannerService.create(request));
    }

    @Operation(summary = "배너 수정")
    @PutMapping("/{id}")
    public ResultDTO<BannerDto> update(@PathVariable Long id, @Valid @RequestBody BannerDto request) {
        return ResultDTO.ok(bannerService.update(id, request));
    }

    @Operation(summary = "배너 정렬순서 변경", description = "드래그 정렬용 정렬순서 단건 변경.")
    @PutMapping("/{id}/sort")
    public ResultDTO<BannerDto> updateSort(
            @PathVariable Long id, @Valid @RequestBody BannerSortRequest request) {
        return ResultDTO.ok(bannerService.updateSortOrder(id, request.sortOrder()));
    }

    @Operation(summary = "배너 삭제")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResultDTO.ok();
    }
}
