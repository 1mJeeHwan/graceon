package org.streamhub.api.v1.sms.adapter;

import org.streamhub.api.v1.sms.entity.SmsStatus;

/**
 * Provider-agnostic SMS send result returned from a {@link SmsSender} (C6 seam).
 *
 * @param sender adapter code used ({@code MOCK} / {@code ALIGO} / {@code SOLAPI})
 * @param status delivery status (mock = always {@code SENT})
 */
public record SmsSendResult(String sender, SmsStatus status) {

    /** A successful mock/real send (status {@link SmsStatus#SENT}). */
    public static SmsSendResult sent(String sender) {
        return new SmsSendResult(sender, SmsStatus.SENT);
    }
}
