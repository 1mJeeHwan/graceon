package org.streamhub.api.v1.siteconfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.siteconfig.dto.SiteConfigData;

/** Admin endpoints for the dynamic UI / site configuration (SYSTEM only). */
@Tag(name = "SiteConfig", description = "운영 사이트 UI 설정(관리자)")
@RestController
@RequestMapping("/v1/site-config")
@PreAuthorize("hasAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM)")
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    public SiteConfigController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @Operation(summary = "사이트 설정 조회", description = "현재 운영 UI 설정을 반환한다(미설정 시 기본값).")
    @GetMapping
    public ResultDTO<SiteConfigData> get() {
        return ResultDTO.ok(siteConfigService.get());
    }

    @Operation(summary = "사이트 설정 저장", description = "운영 UI 설정을 저장한다(단일 레코드 upsert).")
    @PutMapping
    public ResultDTO<SiteConfigData> save(@RequestBody SiteConfigData request) {
        return ResultDTO.ok(siteConfigService.save(request));
    }
}
