package org.streamhub.api.v1.sms.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.sms.entity.SmsStatus;

/**
 * Default SMS sender (demo/test mode). <b>Performs no external call</b> — it logs the intent and
 * reports an immediate {@link SmsStatus#SENT}. The {@code SMS_MESSAGE} row is persisted by
 * {@code SmsService}; this adapter only stands in for the real network dispatch.
 */
@Slf4j
@Component
public class MockSmsSender implements SmsSender {

    private static final String CODE = "MOCK";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        log.info("[DEMO][SMS-mock] {} ({}) → {} chars — not actually sent",
                command.toNumber(), command.channel(), command.content().length());
        return SmsSendResult.sent(CODE);
    }
}
