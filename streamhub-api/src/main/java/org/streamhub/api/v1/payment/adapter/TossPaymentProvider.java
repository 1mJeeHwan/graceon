package org.streamhub.api.v1.payment.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Toss Payments adapter — <b>실키 주입 지점(스텁)</b>. Registered only when
 * {@code app.payment.provider=toss}. The default demo deployment uses {@link MockPaymentProvider}.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossPaymentProvider implements PaymentProvider {

    /** ← 실 {@code test_ck_} 시크릿 키 주입점 (application-prod.yml / 환경변수). */
    @Value("${app.payment.toss.secret-key:}")
    private String secretKey;

    @Override
    public String code() {
        return "TOSS";
    }

    @Override
    public PaymentResult requestPayment(PaymentRequest request) {
        // TODO(실키): 위젯 SDK가 클라에서 결제창을 띄우므로 서버 requestPayment는 보통 no-op.
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }

    @Override
    public PaymentResult approve(PaymentRequest request, String txnId, String maskedCard) {
        // TODO(실키): RestClient POST https://api.tosspayments.com/v1/payments/confirm
        //   Authorization: Basic base64(secretKey + ":") — 샌드박스 test_ck_ 키로 즉시 동작.
        //   body: { paymentKey: txnId, orderId: request.orderNo(), amount: request.amount() }
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }
}
