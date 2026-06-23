package org.streamhub.api.v1.pub.auth.dto;

/** Verified identity returned to the client after a successful 본인인증, to prefill the sign-up form. */
public record CertificationResult(String name, String phone) {
}
