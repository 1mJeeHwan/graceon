package org.streamhub.api.v1.payment.adapter;

/**
 * Payment gateway seam (C4). The default {@link MockPaymentProvider} performs no external
 * call — it synthesises a deterministic {@code txnId} and an immediate approval. Real PG
 * adapters ({@code TossPaymentProvider}, {@code PayPalPaymentProvider},
 * {@code KakaoPaymentProvider}) implement this same interface and are selected via
 * {@code app.payment.provider}; injecting a real sandbox key is the only change needed —
 * {@code PaymentService} depends solely on this interface, never on a concrete PG.
 */
public interface PaymentProvider {

    /** PG code this provider handles ({@code MOCK} / {@code TOSS} / {@code PAYPAL} / {@code KAKAO} / {@code CARD}). */
    String code();

    /**
     * Initiates a payment. Returns a {@code REQUESTED} result carrying the transaction id, plus a
     * {@code redirectUrl} for server-initiated redirect PGs (Kakao/PayPal). For client-SDK PGs
     * (Toss) this is a no-op and the real transaction id arrives later at {@link #approve}.
     */
    PaymentResult requestPayment(PaymentRequest request);

    /**
     * Confirms/approves a previously requested payment.
     *
     * @param request      the original request (amount is the server total)
     * @param requestTxnId the transaction id issued at {@link #requestPayment} and stored on the
     *                     order ({@code tid}/order id); {@code null} for client-SDK PGs where the
     *                     request step issues nothing (Toss)
     * @param clientToken  the completion token the browser supplied after authorising — Toss
     *                     {@code paymentKey}, Kakao {@code pg_token}, PayPal redirect token; for
     *                     mock this equals {@code requestTxnId}
     * @param maskedCard   masked card number for the receipt memo (never the full PAN); may be null
     * @return an {@code APPROVED} result
     */
    PaymentResult approve(
            PaymentRequest request, String requestTxnId, String clientToken, String maskedCard);
}
