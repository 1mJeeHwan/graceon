package org.streamhub.api.v1.payment.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * PayPal adapter — <b>실키 주입 지점(스텁)</b>. Registered only when
 * {@code app.payment.provider=paypal}. The default demo deployment uses {@link MockPaymentProvider}.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "paypal")
public class PayPalPaymentProvider implements PaymentProvider {

    /** ← 실 PayPal 샌드박스 client-id 주입점. */
    @Value("${app.payment.paypal.client-id:}")
    private String clientId;

    /** ← 실 PayPal 샌드박스 secret 주입점. */
    @Value("${app.payment.paypal.secret:}")
    private String secret;

    @Override
    public String code() {
        return "PAYPAL";
    }

    @Override
    public PaymentResult requestPayment(PaymentRequest request) {
        // TODO(실키): RestClient POST https://api-m.sandbox.paypal.com/v2/checkout/orders
        //   (OAuth2 client-credentials 토큰 선발급) — create order, return approval txnId.
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }

    @Override
    public PaymentResult approve(PaymentRequest request, String txnId, String maskedCard) {
        // TODO(실키): RestClient POST .../v2/checkout/orders/{txnId}/capture
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }
}
