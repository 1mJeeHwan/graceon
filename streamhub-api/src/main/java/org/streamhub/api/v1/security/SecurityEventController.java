package org.streamhub.api.v1.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.security.dto.SecurityEventItem;

/**
 * Security-event viewing (SYSTEM only — sensitive monitoring data). Lists authentication
 * failures, access denials, and derived alerts, most recent first.
 */
@Tag(name = "SecurityEvent", description = "보안 이벤트")
@RestController
@RequestMapping("/v1/security-events")
@PreAuthorize("hasAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM)")
public class SecurityEventController {

    private final SecurityEventService securityEventService;

    public SecurityEventController(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    @Operation(summary = "보안 이벤트 목록", description = "인증 실패/접근 거부/보안 알림을 최신순으로 페이지네이션 조회한다.")
    @GetMapping
    public ResultDTO<ResInfinityList<SecurityEventItem>> list(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResultDTO.ok(securityEventService.list(pageNumber, pageSize));
    }
}
