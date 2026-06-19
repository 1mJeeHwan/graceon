package org.streamhub.api.v1.logarchive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;

/**
 * Manual trigger for the weekly log archive job (SYSTEM only — operational/sensitive).
 * Intended for local verification; production runs on the {@link LogArchiveScheduler} cron.
 */
@Tag(name = "LogArchive", description = "로그 아카이브")
@RestController
@RequestMapping("/v1/admin/log-archive")
@PreAuthorize("hasAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM)")
public class LogArchiveController {

    private final LogArchiveService logArchiveService;

    public LogArchiveController(LogArchiveService logArchiveService) {
        this.logArchiveService = logArchiveService;
    }

    @Operation(summary = "로그 아카이브 수동 실행",
            description = "보관 기간이 지난 감사/보안 로그를 S3에 아카이브 후 삭제하고 처리 건수를 반환한다.")
    @PostMapping("/run")
    public ResultDTO<LogArchiveService.ArchiveResult> run() {
        return ResultDTO.ok(logArchiveService.archiveAndPurge());
    }
}
