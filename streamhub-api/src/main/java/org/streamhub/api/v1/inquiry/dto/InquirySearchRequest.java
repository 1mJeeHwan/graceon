package org.streamhub.api.v1.inquiry.dto;

import org.streamhub.api.v1.inquiry.entity.InquiryCategory;
import org.streamhub.api.v1.inquiry.entity.InquiryStatus;

/**
 * Admin inquiry-list filter. Both fields are optional; when neither is given the
 * default listing surfaces OPEN inquiries first (the unanswered queue), then the
 * rest, each group newest first.
 *
 * @param status   optional status filter
 * @param category optional category filter
 */
public record InquirySearchRequest(InquiryStatus status, InquiryCategory category) {
}
