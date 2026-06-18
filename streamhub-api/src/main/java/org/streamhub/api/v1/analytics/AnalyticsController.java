package org.streamhub.api.v1.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.analytics.dto.AnalyticsBreakdownDto;
import org.streamhub.api.v1.analytics.dto.AnalyticsOverviewDto;
import org.streamhub.api.v1.analytics.dto.ContentStatDto;
import org.streamhub.api.v1.analytics.dto.TimeseriesPointDto;

/**
 * Web-analytics dashboard endpoints (콘텐츠 분석, SYSTEM or CHURCH_MANAGER). Surfaces the overview
 * counters, content performance (popular vs underperforming), a daily trend and categorical
 * breakdowns over the demo analytics dataset. The public ingest endpoint lives in
 * {@code AnalyticsPublicController}.
 */
@Tag(name = "Analytics", description = "콘텐츠 분석")
@RestController
@RequestMapping("/v1/analytics")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "분석 개요", description = "총 이벤트/세션/순방문자, 페이지뷰·콘텐츠뷰 수, 평균 체류시간(ms)을 반환합니다.")
    @GetMapping("/overview")
    public ResultDTO<AnalyticsOverviewDto> overview() {
        return ResultDTO.ok(analyticsService.overview());
    }

    @Operation(summary = "콘텐츠 성과", description = "콘텐츠별 조회수·평균 체류시간·최근 조회시각을 조회수 내림차순으로 반환합니다(인기/저조 콘텐츠).")
    @GetMapping("/content-performance")
    public ResultDTO<List<ContentStatDto>> contentPerformance() {
        return ResultDTO.ok(analyticsService.contentPerformance());
    }

    @Operation(summary = "일자별 추이", description = "최근 30일의 일자별 이벤트 수와 세션 수를 반환합니다(빈 날짜는 0으로 채움).")
    @GetMapping("/timeseries")
    public ResultDTO<List<TimeseriesPointDto>> timeseries() {
        return ResultDTO.ok(analyticsService.timeseries());
    }

    @Operation(summary = "분석 분포", description = "디바이스별 분포, 상위 유입경로 6개, 상위 경로 8개를 반환합니다.")
    @GetMapping("/breakdown")
    public ResultDTO<AnalyticsBreakdownDto> breakdown() {
        return ResultDTO.ok(analyticsService.breakdown());
    }
}
