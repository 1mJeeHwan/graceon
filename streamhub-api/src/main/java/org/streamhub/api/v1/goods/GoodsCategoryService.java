package org.streamhub.api.v1.goods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.goods.dto.GoodsCategoryDto;
import org.streamhub.api.v1.goods.entity.GoodsCategory;
import org.streamhub.api.v1.goods.repository.GoodsCategoryRepository;

/**
 * Reads the goods category tree and assembles it into a nested structure for the shop
 * form's category selector. Image keys are resolved to public URLs via {@link StorageService}.
 */
@Service
public class GoodsCategoryService {

    private final GoodsCategoryRepository goodsCategoryRepository;
    private final StorageService storageService;

    public GoodsCategoryService(GoodsCategoryRepository goodsCategoryRepository,
                                StorageService storageService) {
        this.goodsCategoryRepository = goodsCategoryRepository;
        this.storageService = storageService;
    }

    /**
     * Returns the full category tree (roots with nested children), ordered by
     * {@code sort, id}. Each node carries its resolved {@code imageUrl}.
     */
    @Transactional(readOnly = true)
    public List<GoodsCategoryDto> listTree() {
        List<GoodsCategory> all = goodsCategoryRepository.findAllByOrderBySortAscIdAsc();

        Map<Long, GoodsCategoryDto> byId = new LinkedHashMap<>();
        for (GoodsCategory category : all) {
            byId.put(category.getId(),
                    GoodsCategoryDto.of(category, storageService.publicUrl(category.getImageKey())));
        }

        List<GoodsCategoryDto> roots = new ArrayList<>();
        for (GoodsCategoryDto node : byId.values()) {
            if (node.getParentId() == null) {
                roots.add(node);
                continue;
            }
            GoodsCategoryDto parent = byId.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node); // orphaned parent → surface at root rather than drop
            }
        }
        return roots;
    }
}
