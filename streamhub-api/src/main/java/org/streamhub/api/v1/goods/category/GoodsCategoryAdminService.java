package org.streamhub.api.v1.goods.category;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.goods.category.dto.GoodsCategoryNodeDto;
import org.streamhub.api.v1.goods.category.dto.GoodsCategorySaveRequest;
import org.streamhub.api.v1.goods.entity.GoodsCategory;

/**
 * Admin management of the goods category tree. Returns a flat, ordered node list (parent
 * reference + depth) so the frontend can assemble the 3-tier tree, and supports
 * create/update/delete. Reuses the existing seeded {@link GoodsCategory} data.
 */
@Service
public class GoodsCategoryAdminService {

    private final GoodsCategoryAdminRepository goodsCategoryAdminRepository;
    private final ActionLogPublisher actionLogPublisher;

    public GoodsCategoryAdminService(GoodsCategoryAdminRepository goodsCategoryAdminRepository,
                                     ActionLogPublisher actionLogPublisher) {
        this.goodsCategoryAdminRepository = goodsCategoryAdminRepository;
        this.actionLogPublisher = actionLogPublisher;
    }

    /** All categories as a flat list ordered by depth, then sort, then id. */
    @Transactional(readOnly = true)
    public List<GoodsCategoryNodeDto> listAll() {
        return goodsCategoryAdminRepository.findAll().stream()
                .sorted(Comparator.comparing(GoodsCategory::getDepth)
                        .thenComparing(GoodsCategory::getSort)
                        .thenComparing(GoodsCategory::getId))
                .map(GoodsCategoryNodeDto::from)
                .toList();
    }

    /**
     * Creates a category. Depth is derived from the parent (root = 1); a parent at depth 3
     * would exceed the 3-tier limit and is rejected.
     */
    @Transactional
    public GoodsCategoryNodeDto create(GoodsCategorySaveRequest request) {
        int depth = 1;
        if (request.getParentId() != null) {
            GoodsCategory parent = goodsCategoryAdminRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
            depth = parent.getDepth() + 1;
            if (depth > 3) {
                throw new ApiException(ResultCode.INVALID_PARAMETER);
            }
        }
        GoodsCategory category = GoodsCategory.builder()
                .parentId(request.getParentId())
                .name(request.getName())
                .depth(depth)
                .sort(request.getSortOrder())
                .useYn(defaultYn(request.getUseYn()))
                .build();
        GoodsCategory saved = goodsCategoryAdminRepository.save(category);
        actionLogPublisher.publish(
                "GOODS_CATEGORY_CREATE", "GOODS_CATEGORY", String.valueOf(saved.getId()), request.getName());
        return GoodsCategoryNodeDto.from(saved);
    }

    /** Updates editable fields (name/sortOrder/useYn); parent, depth and image are preserved. */
    @Transactional
    public GoodsCategoryNodeDto update(Long id, GoodsCategorySaveRequest request) {
        GoodsCategory category = goodsCategoryAdminRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        category.update(
                category.getParentId(),
                request.getName(),
                category.getDepth(),
                request.getSortOrder() != null ? request.getSortOrder() : category.getSort(),
                category.getImageKey(),
                defaultYn(request.getUseYn()));
        goodsCategoryAdminRepository.saveAndFlush(category);
        actionLogPublisher.publish(
                "GOODS_CATEGORY_UPDATE", "GOODS_CATEGORY", String.valueOf(id), request.getName());
        return GoodsCategoryNodeDto.from(category);
    }

    @Transactional
    public void delete(Long id) {
        GoodsCategory category = goodsCategoryAdminRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        goodsCategoryAdminRepository.delete(category);
        actionLogPublisher.publish(
                "GOODS_CATEGORY_DELETE", "GOODS_CATEGORY", String.valueOf(id), category.getName());
    }

    private String defaultYn(String value) {
        return value == null || value.isBlank() ? "Y" : value;
    }
}
