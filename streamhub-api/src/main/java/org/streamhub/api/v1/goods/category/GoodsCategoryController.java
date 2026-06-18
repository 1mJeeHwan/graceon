package org.streamhub.api.v1.goods.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.goods.category.dto.GoodsCategoryNodeDto;
import org.streamhub.api.v1.goods.category.dto.GoodsCategorySaveRequest;

/**
 * Goods category management endpoints (SYSTEM or CHURCH_MANAGER). Returns a flat,
 * parent-referenced node list so the frontend can render the 3-tier category tree.
 */
@Tag(name = "GoodsCategory", description = "굿즈샵 카테고리 관리")
@RestController
@RequestMapping("/v1/goods-category")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class GoodsCategoryController {

    private final GoodsCategoryAdminService goodsCategoryAdminService;

    public GoodsCategoryController(GoodsCategoryAdminService goodsCategoryAdminService) {
        this.goodsCategoryAdminService = goodsCategoryAdminService;
    }

    @Operation(summary = "카테고리 목록", description = "전체 카테고리를 부모/깊이 정보가 담긴 평탄 목록으로 반환(3단 트리 구성용).")
    @PostMapping("/list")
    public ResultDTO<List<GoodsCategoryNodeDto>> list() {
        return ResultDTO.ok(goodsCategoryAdminService.listAll());
    }

    @Operation(summary = "카테고리 등록")
    @PostMapping
    public ResultDTO<GoodsCategoryNodeDto> create(@Valid @RequestBody GoodsCategorySaveRequest request) {
        return ResultDTO.ok(goodsCategoryAdminService.create(request));
    }

    @Operation(summary = "카테고리 수정", description = "이름/정렬순서/사용여부를 수정한다.")
    @PutMapping("/{id}")
    public ResultDTO<GoodsCategoryNodeDto> update(
            @PathVariable Long id, @Valid @RequestBody GoodsCategorySaveRequest request) {
        return ResultDTO.ok(goodsCategoryAdminService.update(id, request));
    }

    @Operation(summary = "카테고리 삭제")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        goodsCategoryAdminService.delete(id);
        return ResultDTO.ok();
    }
}
