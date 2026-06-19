package org.streamhub.api.v1.pub.goods;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.goods.entity.GoodsImage;
import org.streamhub.api.v1.goods.entity.GoodsItem;
import org.streamhub.api.v1.goods.repository.GoodsImageRepository;
import org.streamhub.api.v1.goods.repository.GoodsItemRepository;
import org.streamhub.api.v1.pub.goods.dto.GoodsDetail;
import org.streamhub.api.v1.pub.goods.dto.GoodsListItem;

/**
 * Public, unauthenticated goods (merchandise) storefront read API for the user site. Exposes only
 * on-sale items ({@code use_yn='Y'}), best-selling then newest first, with optional name keyword
 * filtering. Thumbnail and gallery image keys are resolved to URLs through
 * {@link StorageService#publicUrl} — the same convention used by the admin goods read path.
 */
@Service
public class PublicGoodsService {

    /** Only items flagged on-sale ({@code use_yn='Y'}) are publicly visible. */
    private static final String ON_SALE = "Y";
    private static final String SOLD_OUT_YES = "Y";

    private final GoodsItemRepository goodsItemRepository;
    private final GoodsImageRepository goodsImageRepository;
    private final StorageService storageService;

    public PublicGoodsService(GoodsItemRepository goodsItemRepository,
                              GoodsImageRepository goodsImageRepository,
                              StorageService storageService) {
        this.goodsItemRepository = goodsItemRepository;
        this.goodsImageRepository = goodsImageRepository;
        this.storageService = storageService;
    }

    /**
     * A page of on-sale goods, best-selling then newest first. When {@code keyword} is supplied the
     * results are additionally filtered by a case-insensitive name match.
     */
    @Transactional(readOnly = true)
    public ResInfinityList<GoodsListItem> list(int pageNumber, int pageSize, String keyword) {
        int size = Math.max(1, pageSize);
        Pageable pageable = PageRequest.of(Math.max(0, pageNumber), size);
        Page<GoodsItem> page = StringUtils.hasText(keyword)
                ? goodsItemRepository
                .findByUseYnAndNameContainingIgnoreCaseOrderBySaleCountDescCreatedAtDescIdDesc(
                        ON_SALE, keyword.strip(), pageable)
                : goodsItemRepository
                .findByUseYnOrderBySaleCountDescCreatedAtDescIdDesc(ON_SALE, pageable);
        List<GoodsListItem> contents = page.getContent().stream()
                .map(this::toListItem)
                .toList();
        return ResInfinityList.of(contents, page.getTotalElements(), size);
    }

    /**
     * One on-sale goods item by id.
     *
     * @throws ApiException {@code NOT_FOUND} if missing or not on sale ({@code use_yn != 'Y'})
     */
    @Transactional(readOnly = true)
    public GoodsDetail detail(Long id) {
        GoodsItem item = goodsItemRepository.findByIdAndUseYn(id, ON_SALE)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        List<String> imageUrls = goodsImageRepository.findByItemIdOrderBySortAscIdAsc(id).stream()
                .map(GoodsImage::getS3Key)
                .map(storageService::publicUrl)
                .filter(StringUtils::hasText)
                .toList();
        return new GoodsDetail(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getStock(),
                isSoldOut(item),
                item.getDescription(),
                imageUrls);
    }

    private GoodsListItem toListItem(GoodsItem item) {
        return new GoodsListItem(
                item.getId(),
                item.getName(),
                item.getPrice(),
                storageService.publicUrl(item.getThumbnailKey()),
                isSoldOut(item));
    }

    private boolean isSoldOut(GoodsItem item) {
        return SOLD_OUT_YES.equalsIgnoreCase(item.getSoldOut());
    }
}
