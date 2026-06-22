package org.streamhub.api.v1.announcement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.announcement.dto.AnnouncementDto;

/**
 * Admin management of the site announcement (안내창). Single editable config; SYSTEM/CHURCH_MANAGER
 * write, VIEWER read (via {@code announcement:read}).
 */
@Tag(name = "Announcement", description = "사이트 안내창 관리")
@RestController
@RequestMapping("/v1/announcement")
@PreAuthorize("hasAuthority('announcement:read')")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Operation(summary = "안내창 설정 조회")
    @GetMapping
    public ResultDTO<AnnouncementDto> get() {
        return ResultDTO.ok(announcementService.get());
    }

    @Operation(summary = "안내창 설정 저장", description = "노출 여부·문구·링크를 저장한다(단일 설정 upsert).")
    @PreAuthorize("hasAuthority('announcement:write')")
    @PutMapping
    public ResultDTO<AnnouncementDto> save(@RequestBody AnnouncementDto request) {
        return ResultDTO.ok(announcementService.save(request));
    }
}
