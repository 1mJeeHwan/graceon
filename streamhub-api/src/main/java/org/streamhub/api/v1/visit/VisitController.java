package org.streamhub.api.v1.visit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.visit.dto.DailyCountDto;
import org.streamhub.api.v1.visit.dto.VisitLogDto;
import org.streamhub.api.v1.visit.dto.VisitSearchRequest;
import org.streamhub.api.v1.visit.dto.VisitSummaryDto;

/**
 * Front-site visit statistics endpoints (접속 통계, SYSTEM or CHURCH_MANAGER). Provides the raw
 * visit list, a daily-trend aggregate and a dashboard summary over the demo traffic dataset.
 */
@Tag(name = "Visit", description = "접속 통계")
@RestController
@RequestMapping("/v1/visit")
@PreAuthorize("hasAuthority('visit:read')") // class default = read; read-only statistics controller
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "접속 로그 목록", description = "기간/디바이스/검색어 필터로 접속 로그를 최신순(최대 500건)으로 조회합니다.")
    @PostMapping("/list")
    public ResultDTO<List<VisitLogDto>> list(@RequestBody(required = false) VisitSearchRequest request) {
        return ResultDTO.ok(visitService.list(request));
    }

    @Operation(summary = "일자별 접속 통계", description = "요청 기간의 일자별 접속 수를 반환합니다(기본 최근 30일, 빈 날짜는 0으로 채움).")
    @PostMapping("/daily")
    public ResultDTO<List<DailyCountDto>> daily(@RequestBody(required = false) VisitSearchRequest request) {
        return ResultDTO.ok(visitService.daily(request));
    }

    @Operation(summary = "접속 통계 요약", description = "총 접속 수, 오늘 접속 수, 추정 순방문자 수, 상위 경로 5개, 디바이스별 분포를 반환합니다.")
    @GetMapping("/summary")
    public ResultDTO<VisitSummaryDto> summary() {
        return ResultDTO.ok(visitService.summary());
    }
}
