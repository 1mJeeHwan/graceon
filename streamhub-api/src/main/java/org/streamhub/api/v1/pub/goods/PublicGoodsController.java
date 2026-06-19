package org.streamhub.api.v1.pub.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.goods.dto.GoodsDetail;
import org.streamhub.api.v1.pub.goods.dto.GoodsListItem;

/**
 * Public, unauthenticated goods (merchandise) storefront read API consumed by the user site. Mapped
 * under {@code /pub/**}, which is permitAll in {@code SecurityConfig}, so no auth wiring is needed.
 * Exposes only on-sale ({@code use_yn='Y'}) items.
 */
@Tag(name = "Public Goods", description = "사용자 사이트용 공개 굿즈 API (인증 불필요, 판매중만)")
@RestController
@RequestMapping("/pub/v1/goods")
public class PublicGoodsController {

    private final PublicGoodsService publicGoodsService;

    public PublicGoodsController(PublicGoodsService publicGoodsService) {
        this.publicGoodsService = publicGoodsService;
    }

    @Operation(summary = "공개 굿즈 목록",
            description = "판매중(use_yn='Y') 굿즈를 판매량·최신순으로 페이징해 반환한다. keyword로 상품명 검색(선택).")
    @GetMapping
    public ResultDTO<ResInfinityList<GoodsListItem>> list(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "12") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultDTO.ok(publicGoodsService.list(pageNumber, pageSize, keyword));
    }

    @Operation(summary = "공개 굿즈 상세", description = "판매중 굿즈의 상세를 반환한다. 없거나 비공개면 NOT_FOUND.")
    @GetMapping("/{id}")
    public ResultDTO<GoodsDetail> detail(@PathVariable Long id) {
        return ResultDTO.ok(publicGoodsService.detail(id));
    }
}
