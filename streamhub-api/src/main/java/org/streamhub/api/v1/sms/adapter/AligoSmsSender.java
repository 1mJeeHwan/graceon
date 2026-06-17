package org.streamhub.api.v1.sms.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aligo SMS adapter — <b>실키 주입 지점(스텁)</b>. Registered only when {@code app.sms.sender=aligo}.
 * The default demo deployment uses {@link MockSmsSender}.
 */
@Component
@ConditionalOnProperty(name = "app.sms.sender", havingValue = "aligo")
public class AligoSmsSender implements SmsSender {

    /** ← 실 Aligo API key 주입점. */
    @Value("${app.sms.aligo.api-key:}")
    private String apiKey;

    /** ← 실 Aligo user id 주입점. */
    @Value("${app.sms.aligo.user-id:}")
    private String userId;

    @Override
    public String code() {
        return "ALIGO";
    }

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        // TODO(실키): RestClient POST https://apis.aligo.in/send/  (multipart form)
        //   key=apiKey, user_id=userId, sender={발신번호-사전등록필수}, receiver, msg, msg_type=SMS|LMS
        //   ※ 발신번호는 Aligo 콘솔에 사전 등록되어 있어야 발송 가능.
        throw new UnsupportedOperationException("실 SMS 미연동(데모) — app.sms.sender=mock 사용");
    }
}
