package org.streamhub.api.v1.sms.adapter;

/**
 * SMS dispatch seam (C6). The default {@link MockSmsSender} performs no external call — the
 * caller ({@code SmsService}) persists the {@code SMS_MESSAGE} row regardless. Real adapters
 * ({@code AligoSmsSender}, {@code SolapiSmsSender}) implement this same interface and are
 * selected via {@code app.sms.sender}; the sender number/template are the provider's concern.
 */
public interface SmsSender {

    /** Adapter code this sender reports ({@code MOCK} / {@code ALIGO} / {@code SOLAPI}). */
    String code();

    /** Dispatches the message (mock = no external call, immediate {@code SENT}). */
    SmsSendResult send(SmsSendCommand command);
}
