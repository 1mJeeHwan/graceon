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

    /** Initiates a payment (returns a {@code REQUESTED} result carrying the transaction id). */
    PaymentResult requestPayment(PaymentRequest request);

    /**
     * Confirms/approves a previously requested payment.
     *
     * @param request    the original request (amount is the server total)
     * @param txnId      the transaction id from {@link #requestPayment}
     * @param maskedCard masked card number for the receipt memo (never the full PAN); may be null
     * @return an {@code APPROVED} result
     */
    PaymentResult approve(PaymentRequest request, String txnId, String maskedCard);
}
