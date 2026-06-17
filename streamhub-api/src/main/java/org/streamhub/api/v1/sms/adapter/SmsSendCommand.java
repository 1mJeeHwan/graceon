package org.streamhub.api.v1.sms.adapter;

import org.streamhub.api.v1.sms.entity.SmsChannel;

/**
 * Provider-agnostic SMS send command passed into a {@link SmsSender} (C6 seam).
 *
 * @param toNumber recipient number (already masked by the service before reaching the sender)
 * @param content  message body
 * @param channel  resolved channel ({@code SMS} / {@code LMS})
 */
public record SmsSendCommand(String toNumber, String content, SmsChannel channel) {
}
