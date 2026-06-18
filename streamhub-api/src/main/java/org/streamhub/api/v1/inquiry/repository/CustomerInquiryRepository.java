package org.streamhub.api.v1.inquiry.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.inquiry.entity.CustomerInquiry;
import org.streamhub.api.v1.inquiry.entity.InquiryCategory;
import org.streamhub.api.v1.inquiry.entity.InquiryStatus;

/** JPA repository for {@link CustomerInquiry} (1:1 customer support inquiries). */
public interface CustomerInquiryRepository extends JpaRepository<CustomerInquiry, Long> {

    List<CustomerInquiry> findByStatus(InquiryStatus status);

    List<CustomerInquiry> findByCategory(InquiryCategory category);

    List<CustomerInquiry> findByStatusAndCategory(InquiryStatus status, InquiryCategory category);
}
