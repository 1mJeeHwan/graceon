package org.streamhub.api.v1.payment.adapter;

/**
 * Provider-agnostic payment request passed into a {@link PaymentProvider} (C4 payment seam).
 *
 * <p>The {@code amount} is always the server-computed {@code order.total} — the client never
 * supplies the amount (spec §3.5).
 *
 * @param orderNo  business order number ({@code YYYYMMDD-XXXXXX})
 * @param amount   server-computed total to charge
 * @param provider PG code requested ({@code MOCK}/{@code TOSS}/{@code PAYPAL}/{@code KAKAO}/{@code CARD})
 */
public record PaymentRequest(String orderNo, Long amount, String provider) {
}
