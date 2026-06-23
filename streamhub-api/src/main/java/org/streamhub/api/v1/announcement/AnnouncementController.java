package org.streamhub.api.v1.announcement;

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
import org.streamhub.api.v1.announcement.dto.AnnouncementDto;
import org.streamhub.api.v1.announcement.dto.AnnouncementSearchRequest;
import org.streamhub.api.v1.announcement.dto.AnnouncementSortRequest;

/**
 * Admin management of modal-ad announcements (안내창), managed like banners. SYSTEM/CHURCH_MANAGER
 * write, VIEWER read (via {@code announcement:read}). Each row is an image popup ad shown on the
 * user site.
 */
@Tag(name = "Announcement", description = "사이트 안내창(모달 광고) 관리")
@RestController
@RequestMapping("/v1/announcement")
@PreAuthorize("hasAuthority('announcement:read')") // class default = read; mutations require write
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Operation(summary = "안내창 목록", description = "노출여부 필터, 정렬순서 오름차순 목록.")
    @PostMapping("/list")
    public ResultDTO<List<AnnouncementDto>> list(@RequestBody AnnouncementSearchRequest request) {
        return ResultDTO.ok(announcementService.list(request));
    }

    @Operation(summary = "안내창 상세")
    @GetMapping("/{id}")
    public ResultDTO<AnnouncementDto> detail(@PathVariable Long id) {
        return ResultDTO.ok(announcementService.getDetail(id));
    }

    @Operation(summary = "안내창 등록")
    @PreAuthorize("hasAuthority('announcement:write')")
    @PostMapping
    public ResultDTO<AnnouncementDto> create(@Valid @RequestBody AnnouncementDto request) {
        return ResultDTO.ok(announcementService.create(request));
    }

    @Operation(summary = "안내창 수정")
    @PreAuthorize("hasAuthority('announcement:write')")
    @PutMapping("/{id}")
    public ResultDTO<AnnouncementDto> update(
            @PathVariable Long id, @Valid @RequestBody AnnouncementDto request) {
        return ResultDTO.ok(announcementService.update(id, request));
    }

    @Operation(summary = "안내창 정렬순서 변경", description = "정렬순서 단건 변경.")
    @PreAuthorize("hasAuthority('announcement:write')")
    @PutMapping("/{id}/sort")
    public ResultDTO<AnnouncementDto> updateSort(
            @PathVariable Long id, @Valid @RequestBody AnnouncementSortRequest request) {
        return ResultDTO.ok(announcementService.updateSortOrder(id, request.sortOrder()));
    }

    @Operation(summary = "안내창 삭제")
    @PreAuthorize("hasAuthority('announcement:write')")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResultDTO.ok();
    }
}
