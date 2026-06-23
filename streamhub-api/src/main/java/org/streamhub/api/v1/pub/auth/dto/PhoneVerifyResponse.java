package org.streamhub.api.v1.pub.auth.dto;

/**
 * Result of requesting an SMS authentication code.
 *
 * @param expiresIn seconds until the code expires
 * @param devCode   the generated code, surfaced only in demo mode (no real SMS gateway is wired).
 *                  Null once a real SMS provider is configured ({@code app.verification.expose-code=false}).
 */
public record PhoneVerifyResponse(long expiresIn, String devCode) {
}
