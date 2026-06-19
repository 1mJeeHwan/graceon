package org.streamhub.api.v1.goods.stock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.goods.stock.dto.GoodsStockDto;
import org.streamhub.api.v1.goods.stock.dto.GoodsStockSearchRequest;
import org.streamhub.api.v1.goods.stock.dto.GoodsStockUpdateRequest;

/**
 * Goods stock (inventory) management endpoints (SYSTEM or CHURCH_MANAGER). Provides a
 * filterable stock listing plus per-item stock edits and a sold-out toggle.
 */
@Tag(name = "GoodsStock", description = "굿즈샵 재고 관리")
@RestController
@RequestMapping("/v1/goods-stock")
@PreAuthorize("hasAuthority('goods:read')") // class default = read; mutations require goods:write
public class GoodsStockController {

    private final GoodsStockService goodsStockService;

    public GoodsStockController(GoodsStockService goodsStockService) {
        this.goodsStockService = goodsStockService;
    }

    @Operation(summary = "재고 목록", description = "키워드/재고부족 필터 및 재고 오름차순 정렬을 지원하는 재고 목록.")
    @PostMapping("/list")
    public ResultDTO<List<GoodsStockDto>> list(@RequestBody GoodsStockSearchRequest request) {
        return ResultDTO.ok(goodsStockService.list(request));
    }

    @Operation(summary = "재고 수정", description = "재고수량(및 선택적으로 알림기준수량)을 수정한다.")
    @PreAuthorize("hasAuthority('goods:write')")
    @PutMapping("/{id}/stock")
    public ResultDTO<GoodsStockDto> updateStock(
            @PathVariable Long id, @RequestBody GoodsStockUpdateRequest request) {
        return ResultDTO.ok(goodsStockService.updateStock(id, request));
    }

    @Operation(summary = "품절 토글", description = "품절 여부(Y/N)를 전환한다.")
    @PreAuthorize("hasAuthority('goods:write')")
    @PutMapping("/{id}/soldout")
    public ResultDTO<GoodsStockDto> toggleSoldOut(@PathVariable Long id) {
        return ResultDTO.ok(goodsStockService.toggleSoldOut(id));
    }
}
