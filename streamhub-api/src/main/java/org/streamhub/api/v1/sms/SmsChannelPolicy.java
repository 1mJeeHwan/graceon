package org.streamhub.api.v1.sms;

import java.nio.charset.Charset;
import org.streamhub.api.v1.sms.entity.SmsChannel;

/**
 * Single source of truth for the SMS/LMS channel decision (Korean carrier convention): a body
 * over {@value #LMS_THRESHOLD_BYTES} EUC-KR bytes is billed as LMS, otherwise SMS.
 *
 * <p>Both the runtime send path ({@link SmsService}) and the demo seeder classify through here so
 * a seeded history row carries the exact channel a live send would have produced (no UTF-8 vs
 * EUC-KR drift).
 */
public final class SmsChannelPolicy {

    /** EUC-KR byte threshold separating SMS from LMS. */
    public static final int LMS_THRESHOLD_BYTES = 90;
    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private SmsChannelPolicy() {
    }

    /** {@code > 90} EUC-KR bytes ⇒ LMS, else SMS; a null body defaults to SMS. */
    public static SmsChannel resolve(String content) {
        if (content == null) {
            return SmsChannel.SMS;
        }
        return content.getBytes(EUC_KR).length > LMS_THRESHOLD_BYTES ? SmsChannel.LMS : SmsChannel.SMS;
    }
}
