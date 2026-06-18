package org.streamhub.api.v1.pub.order.dto;

/**
 * Phase-1 result: everything the browser needs to open the Toss v2 payment window. The order is
 * already created server-side ({@code orderNo}/{@code amount} are authoritative), so the window —
 * and Toss's own confirm check — are bound to a server-fixed amount.
 *
 * @param orderNo     business order number, passed to the window as {@code orderId}
 * @param orderName   human-readable order name shown in the window
 * @param amount      server-computed total (KRW)
 * @param provider    PG code that will handle the payment
 * @param clientKey   PG client (publishable) key for the browser SDK (Toss); null for redirect PGs
 * @param customerKey member-scoped customer key for the window
 * @param redirectUrl for server-initiated redirect PGs (Kakao/PayPal), the URL to navigate to;
 *                    null for client-SDK PGs (Toss)
 */
public record MemberPaymentPrepareResult(
        String orderNo,
        String orderName,
        Long amount,
        String provider,
        String clientKey,
        String customerKey,
        String redirectUrl) {
}
