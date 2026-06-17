package org.streamhub.api.v1.sms.entity;

/** SMS send state (mock always {@code SENT}). Stored via {@code @Enumerated(STRING)}. */
public enum SmsStatus {
    QUEUED,
    SENT,
    FAILED
}
