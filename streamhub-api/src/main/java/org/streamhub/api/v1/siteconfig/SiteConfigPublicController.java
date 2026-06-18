package org.streamhub.api.v1.siteconfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.siteconfig.dto.SiteConfigData;

/**
 * Public, read-only site configuration consumed by the user site (theme/accent/announcement/
 * home layout). Lives under {@code /pub/**}, which {@code SecurityConfig} permits without auth.
 * Kept separate from the admin controller so it carries no {@code @PreAuthorize}.
 */
@Tag(name = "SiteConfigPublic", description = "운영 사이트 UI 설정(공개)")
@RestController
@RequestMapping("/pub/v1/site-config")
public class SiteConfigPublicController {

    private final SiteConfigService siteConfigService;

    public SiteConfigPublicController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @Operation(summary = "공개 사이트 설정", description = "운영 사이트가 렌더링에 사용하는 UI 설정을 반환한다.")
    @GetMapping
    public ResultDTO<SiteConfigData> get() {
        return ResultDTO.ok(siteConfigService.get());
    }
}
