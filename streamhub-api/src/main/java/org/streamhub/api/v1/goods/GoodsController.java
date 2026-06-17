package org.streamhub.api.v1.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.goods.dto.GoodsBulkUpdateRequest;
import org.streamhub.api.v1.goods.dto.GoodsCategoryDto;
import org.streamhub.api.v1.goods.dto.GoodsCreateRequest;
import org.streamhub.api.v1.goods.dto.GoodsDetail;
import org.streamhub.api.v1.goods.dto.GoodsListItem;
import org.streamhub.api.v1.goods.dto.GoodsSearchRequest;
import org.streamhub.api.v1.goods.dto.UploadResponse;

/**
 * Goods (merchandise) management endpoints (SYSTEM or CHURCH_MANAGER).
 */
@Tag(name = "Goods", description = "굿즈샵 상품 관리")
@RestController
@RequestMapping("/v1/goods")
@PreAuthorize("hasAnyAuthority(T(org.streamhub.api.base.security.AuthoritiesConstants).SYSTEM, "
        + "T(org.streamhub.api.base.security.AuthoritiesConstants).CHURCH_MANAGER)")
public class GoodsController {

    private final GoodsService goodsService;
    private final GoodsCategoryService goodsCategoryService;
    private final StorageService storageService;

    public GoodsController(GoodsService goodsService,
                           GoodsCategoryService goodsCategoryService,
                           StorageService storageService) {
        this.goodsService = goodsService;
        this.goodsCategoryService = goodsCategoryService;
        this.storageService = storageService;
    }

    @Operation(summary = "굿즈 목록", description = "검색/필터/페이지네이션된 굿즈 상품 목록.")
    @PostMapping("/list")
    public ResultDTO<ResInfinityList<GoodsListItem>> list(@RequestBody GoodsSearchRequest request) {
        return ResultDTO.ok(goodsService.list(request));
    }

    @Operation(summary = "굿즈 상세")
    @GetMapping("/{id}")
    public ResultDTO<GoodsDetail> detail(@PathVariable Long id) {
        return ResultDTO.ok(goodsService.getDetail(id));
    }

    @Operation(summary = "굿즈 등록")
    @PostMapping
    public ResultDTO<GoodsDetail> create(@Valid @RequestBody GoodsCreateRequest request) {
        return ResultDTO.ok(goodsService.create(request));
    }

    @Operation(summary = "굿즈 수정")
    @PutMapping("/{id}")
    public ResultDTO<GoodsDetail> update(
            @PathVariable Long id, @Valid @RequestBody GoodsCreateRequest request) {
        return ResultDTO.ok(goodsService.update(id, request));
    }

    @Operation(summary = "굿즈 삭제")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return ResultDTO.ok();
    }

    @Operation(summary = "굿즈 인라인 일괄수정", description = "AG Grid에서 변경된 행만 반영. 반영 행수를 반환.")
    @PutMapping("/bulk")
    public ResultDTO<Integer> bulkUpdate(@Valid @RequestBody GoodsBulkUpdateRequest request) {
        return ResultDTO.ok(goodsService.bulkUpdate(request));
    }

    @Operation(summary = "굿즈 이미지 업로드", description = "썸네일/추가 이미지를 스토리지에 업로드하고 key/url을 반환한다.")
    @PostMapping("/upload")
    public ResultDTO<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String key = storageService.upload(file, "goods");
        return ResultDTO.ok(new UploadResponse(key, storageService.publicUrl(key)));
    }

    @Operation(summary = "굿즈 카테고리 트리", description = "굿즈 등록 폼의 분류 선택용 트리.")
    @GetMapping("/categories")
    public ResultDTO<List<GoodsCategoryDto>> categories() {
        return ResultDTO.ok(goodsCategoryService.listTree());
    }
}
