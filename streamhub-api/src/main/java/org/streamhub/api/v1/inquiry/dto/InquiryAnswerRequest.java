package org.streamhub.api.v1.inquiry.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Operator answer payload for a 1:1 customer inquiry.
 *
 * @param answerContent the reply text shown to the member
 */
public record InquiryAnswerRequest(@NotBlank String answerContent) {
}
