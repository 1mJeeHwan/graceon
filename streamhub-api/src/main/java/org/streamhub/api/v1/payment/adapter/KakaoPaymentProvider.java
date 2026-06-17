package org.streamhub.api.v1.payment.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kakao Pay adapter — <b>실키 주입 지점(스텁)</b>. Registered only when
 * {@code app.payment.provider=kakao}. The default demo deployment uses {@link MockPaymentProvider}.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "kakao")
public class KakaoPaymentProvider implements PaymentProvider {

    /** ← 실 KakaoPay {@code SECRET_KEY} (Open API) 주입점. */
    @Value("${app.payment.kakao.secret-key:}")
    private String secretKey;

    @Override
    public String code() {
        return "KAKAO";
    }

    @Override
    public PaymentResult requestPayment(PaymentRequest request) {
        // TODO(실키): RestClient POST https://open-api.kakaopay.com/online/v1/payment/ready
        //   Authorization: SECRET_KEY {secretKey} — returns tid(txnId) + redirect url.
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }

    @Override
    public PaymentResult approve(PaymentRequest request, String txnId, String maskedCard) {
        // TODO(실키): RestClient POST .../online/v1/payment/approve  (tid + pg_token)
        throw new UnsupportedOperationException("실 PG 미연동(데모) — app.payment.provider=mock 사용");
    }
}
