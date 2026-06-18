package org.streamhub.api.v1.goods.inquiry;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.goods.inquiry.dto.GoodsInquiryAnswerRequest;
import org.streamhub.api.v1.goods.inquiry.dto.GoodsInquiryDto;
import org.streamhub.api.v1.goods.inquiry.dto.GoodsInquirySearchRequest;
import org.streamhub.api.v1.goods.inquiry.entity.GoodsInquiry;
import org.streamhub.api.v1.goods.inquiry.repository.GoodsInquiryRepository;

/** Goods Q&A management: admin listing, detail, answering and deletion. */
@Service
public class GoodsInquiryService {

    private final GoodsInquiryRepository goodsInquiryRepository;

    public GoodsInquiryService(GoodsInquiryRepository goodsInquiryRepository) {
        this.goodsInquiryRepository = goodsInquiryRepository;
    }

    /** Admin listing: newest first, optionally filtered by answer status. */
    @Transactional(readOnly = true)
    public List<GoodsInquiryDto> list(GoodsInquirySearchRequest request) {
        List<GoodsInquiry> inquiries = (request != null && request.getAnswerStatus() != null)
                ? goodsInquiryRepository.findByAnswerStatusOrderByIdDesc(request.getAnswerStatus())
                : goodsInquiryRepository.findAllByOrderByIdDesc();
        return inquiries.stream().map(GoodsInquiryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public GoodsInquiryDto detail(Long id) {
        GoodsInquiry inquiry = goodsInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return GoodsInquiryDto.from(inquiry);
    }

    @Transactional
    public GoodsInquiryDto answer(Long id, GoodsInquiryAnswerRequest request) {
        GoodsInquiry inquiry = goodsInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        inquiry.answer(request.getAnswerContent());
        goodsInquiryRepository.saveAndFlush(inquiry);
        return GoodsInquiryDto.from(inquiry);
    }

    @Transactional
    public void delete(Long id) {
        GoodsInquiry inquiry = goodsInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        goodsInquiryRepository.delete(inquiry);
    }
}
