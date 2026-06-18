package org.streamhub.api.v1.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.analytics.dto.EventIngestRequest;

/**
 * Public, unauthenticated analytics ingest consumed by the user site ({@code streamhub-user-web}).
 * Mapped under {@code /pub/**}, which is permitAll in
 * {@link org.streamhub.api.base.security.SecurityConfig}. This is hit directly by the browser, so
 * the service parses every field defensively and never throws on malformed input — each event is a
 * single cheap insert.
 */
@Tag(name = "AnalyticsPublic", description = "사용자 사이트용 분석 이벤트 수집 (인증 불필요)")
@RestController
@RequestMapping("/pub/v1/events")
public class AnalyticsPublicController {

    private final AnalyticsService analyticsService;

    public AnalyticsPublicController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "분석 이벤트 수집", description = "페이지뷰/콘텐츠뷰/세션시작 이벤트 1건을 적재한다. 잘못된 입력은 기본값으로 보정.")
    @PostMapping
    public ResultDTO<Void> ingest(@RequestBody(required = false) EventIngestRequest request) {
        analyticsService.ingest(request);
        return ResultDTO.ok();
    }

    @Operation(summary = "분석 이벤트 일괄 수집", description = "여러 이벤트를 한 번에 적재한다. 잘못된 입력은 기본값으로 보정.")
    @PostMapping("/batch")
    public ResultDTO<Void> ingestBatch(@RequestBody(required = false) List<EventIngestRequest> requests) {
        analyticsService.ingestBatch(requests);
        return ResultDTO.ok();
    }
}
