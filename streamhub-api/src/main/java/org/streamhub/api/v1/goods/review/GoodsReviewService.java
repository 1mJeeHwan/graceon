package org.streamhub.api.v1.goods.review;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.goods.review.dto.GoodsReviewDisplayRequest;
import org.streamhub.api.v1.goods.review.dto.GoodsReviewDto;
import org.streamhub.api.v1.goods.review.dto.GoodsReviewRatingRequest;
import org.streamhub.api.v1.goods.review.dto.GoodsReviewSearchRequest;
import org.streamhub.api.v1.goods.review.entity.GoodsReview;
import org.streamhub.api.v1.goods.review.repository.GoodsReviewRepository;

/** Goods review management: admin listing, display toggle, rating update and deletion. */
@Service
public class GoodsReviewService {

    private final GoodsReviewRepository goodsReviewRepository;

    public GoodsReviewService(GoodsReviewRepository goodsReviewRepository) {
        this.goodsReviewRepository = goodsReviewRepository;
    }

    /** Admin listing: newest first, optionally filtered by display flag. */
    @Transactional(readOnly = true)
    public List<GoodsReviewDto> list(GoodsReviewSearchRequest request) {
        List<GoodsReview> reviews = (request != null && request.getDisplayYn() != null
                && !request.getDisplayYn().isBlank())
                ? goodsReviewRepository.findByDisplayYnOrderByIdDesc(request.getDisplayYn())
                : goodsReviewRepository.findAllByOrderByIdDesc();
        return reviews.stream().map(GoodsReviewDto::from).toList();
    }

    @Transactional
    public GoodsReviewDto changeDisplay(Long id, GoodsReviewDisplayRequest request) {
        GoodsReview review = goodsReviewRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        review.changeDisplayYn(request.getDisplayYn());
        goodsReviewRepository.saveAndFlush(review);
        return GoodsReviewDto.from(review);
    }

    @Transactional
    public GoodsReviewDto changeRating(Long id, GoodsReviewRatingRequest request) {
        GoodsReview review = goodsReviewRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        review.changeRating(request.getRating());
        goodsReviewRepository.saveAndFlush(review);
        return GoodsReviewDto.from(review);
    }

    @Transactional
    public void delete(Long id) {
        GoodsReview review = goodsReviewRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        goodsReviewRepository.delete(review);
    }
}
