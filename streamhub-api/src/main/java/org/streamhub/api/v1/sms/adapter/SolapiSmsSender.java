package org.streamhub.api.v1.sms.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SOLAPI (CoolSMS) adapter — <b>실키 주입 지점(스텁)</b>. Registered only when
 * {@code app.sms.sender=solapi}. The default demo deployment uses {@link MockSmsSender}.
 */
@Component
@ConditionalOnProperty(name = "app.sms.sender", havingValue = "solapi")
public class SolapiSmsSender implements SmsSender {

    /** ← 실 SOLAPI API key 주입점. */
    @Value("${app.sms.solapi.api-key:}")
    private String apiKey;

    /** ← 실 SOLAPI API secret 주입점. */
    @Value("${app.sms.solapi.api-secret:}")
    private String apiSecret;

    @Override
    public String code() {
        return "SOLAPI";
    }

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        // TODO(실키): RestClient POST https://api.solapi.com/messages/v4/send
        //   Authorization: HMAC-SHA256(apiKey, apiSecret, date, salt)
        //   body: { message: { to, from:{발신번호-사전등록필수}, text, type:SMS|LMS } }
        throw new UnsupportedOperationException("실 SMS 미연동(데모) — app.sms.sender=mock 사용");
    }
}
