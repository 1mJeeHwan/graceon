package org.streamhub.api.v1.inquiry;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.inquiry.dto.InquiryAnswerRequest;
import org.streamhub.api.v1.inquiry.dto.InquiryDto;
import org.streamhub.api.v1.inquiry.dto.InquirySearchRequest;
import org.streamhub.api.v1.inquiry.entity.CustomerInquiry;
import org.streamhub.api.v1.inquiry.entity.InquiryStatus;
import org.streamhub.api.v1.inquiry.repository.CustomerInquiryRepository;

/**
 * 1:1 customer inquiry management: a status/category-filtered listing plus the answer,
 * close, and delete operations an operator works through. The demo dataset is small, so
 * the listing loads all matching rows and orders them in memory — no pagination needed.
 */
@Service
public class InquiryService {

    private final CustomerInquiryRepository customerInquiryRepository;

    public InquiryService(CustomerInquiryRepository customerInquiryRepository) {
        this.customerInquiryRepository = customerInquiryRepository;
    }

    /**
     * Admin listing. Applies the optional status/category filter, then orders newest first.
     * When no status filter is given, OPEN inquiries (the unanswered queue) are surfaced
     * ahead of the rest so operators see pending work first.
     */
    @Transactional(readOnly = true)
    public List<InquiryDto> list(InquirySearchRequest request) {
        InquiryStatus status = request != null ? request.status() : null;
        List<CustomerInquiry> inquiries = findFiltered(request);
        Comparator<CustomerInquiry> byCreatedDesc =
                Comparator.comparing(CustomerInquiry::getCreatedAt).reversed();
        Comparator<CustomerInquiry> ordering = status != null
                ? byCreatedDesc
                : Comparator.comparing((CustomerInquiry i) -> i.getStatus() == InquiryStatus.OPEN ? 0 : 1)
                        .thenComparing(byCreatedDesc);
        return inquiries.stream()
                .sorted(ordering)
                .map(InquiryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryDto detail(Long id) {
        CustomerInquiry inquiry = customerInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return InquiryDto.from(inquiry);
    }

    @Transactional
    public InquiryDto answer(Long id, InquiryAnswerRequest request) {
        CustomerInquiry inquiry = customerInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        inquiry.answer(request.answerContent());
        customerInquiryRepository.saveAndFlush(inquiry);
        return InquiryDto.from(inquiry);
    }

    @Transactional
    public InquiryDto close(Long id) {
        CustomerInquiry inquiry = customerInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        inquiry.close();
        customerInquiryRepository.saveAndFlush(inquiry);
        return InquiryDto.from(inquiry);
    }

    @Transactional
    public void delete(Long id) {
        CustomerInquiry inquiry = customerInquiryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        customerInquiryRepository.delete(inquiry);
    }

    // --- helpers -----------------------------------------------------------

    private List<CustomerInquiry> findFiltered(InquirySearchRequest request) {
        if (request == null) {
            return customerInquiryRepository.findAll();
        }
        if (request.status() != null && request.category() != null) {
            return customerInquiryRepository.findByStatusAndCategory(request.status(), request.category());
        }
        if (request.status() != null) {
            return customerInquiryRepository.findByStatus(request.status());
        }
        if (request.category() != null) {
            return customerInquiryRepository.findByCategory(request.category());
        }
        return customerInquiryRepository.findAll();
    }
}
