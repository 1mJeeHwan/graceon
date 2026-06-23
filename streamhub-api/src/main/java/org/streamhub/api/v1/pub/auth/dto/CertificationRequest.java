package org.streamhub.api.v1.pub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Identity-verification confirm: the {@code imp_uid} returned by the Iamport certification popup. */
public record CertificationRequest(
        @NotBlank(message = "본인인증 정보가 없습니다") String impUid) {
}
