package org.streamhub.api.v1.sms.entity;

/** SMS channel, decided by body byte length ({@code >90byte = LMS}). Stored via {@code @Enumerated(STRING)}. */
public enum SmsChannel {
    SMS,
    LMS
}
