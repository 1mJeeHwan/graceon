package org.streamhub.api.v1.banner;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.banner.dto.BannerDto;
import org.streamhub.api.v1.banner.entity.BannerPosition;

/**
 * Public banner feed for the user site (active banners only). Lives under {@code /pub/**},
 * which {@code SecurityConfig} permits without auth — separate from the admin
 * {@code BannerController} so it carries no {@code @PreAuthorize}.
 */
@Tag(name = "BannerPublic", description = "운영 사이트 배너(공개)")
@RestController
@RequestMapping("/pub/v1/banners")
public class BannerPublicController {

    private final BannerService bannerService;

    public BannerPublicController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @Operation(summary = "공개 배너 목록", description = "노출 기간 내·사용중 배너만 정렬순으로 반환. position으로 위치 필터.")
    @GetMapping
    public ResultDTO<List<BannerDto>> list(@RequestParam(required = false) BannerPosition position) {
        return ResultDTO.ok(bannerService.listPublic(position));
    }
}
