package org.streamhub.api.v1.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.streamhub.api.v1.sms.entity.SmsChannel;

/**
 * Unit tests for {@link SmsChannelPolicy} — the single source of truth for the SMS/LMS decision
 * shared by the runtime send path and the demo seeder. The key invariant under test: classification
 * is by EUC-KR bytes, so a body that straddles the 90-byte boundary under EUC-KR but not UTF-8
 * (Korean is 2 bytes in EUC-KR, 3 in UTF-8) is classified consistently everywhere.
 */
class SmsChannelPolicyTest {

    @DisplayName("null 본문은 기본 SMS")
    @Test
    void resolve_null_isSms() {
        assertThat(SmsChannelPolicy.resolve(null)).isEqualTo(SmsChannel.SMS);
    }

    @DisplayName("EUC-KR 90바이트 이하 본문은 SMS")
    @Test
    void resolve_shortKorean_isSms() {
        String content = "결제가 완료되었습니다 감사합니다"; // < 90 EUC-KR bytes
        assertThat(content.getBytes(java.nio.charset.Charset.forName("EUC-KR")).length)
                .isLessThanOrEqualTo(90);
        assertThat(SmsChannelPolicy.resolve(content)).isEqualTo(SmsChannel.SMS);
    }

    @DisplayName("EUC-KR 90바이트 초과 본문은 LMS")
    @Test
    void resolve_longKorean_isLms() {
        String content = "한글은 EUC-KR에서 글자당 2바이트이므로 마흔여섯 글자를 넘어가는 충분히 긴 안내 문구는 엘엠에스로 분류되어야 정상입니다 확인용";
        assertThat(content.getBytes(java.nio.charset.Charset.forName("EUC-KR")).length)
                .isGreaterThan(90);
        assertThat(SmsChannelPolicy.resolve(content)).isEqualTo(SmsChannel.LMS);
    }

    /**
     * Boundary parity: 41 Korean characters = 82 EUC-KR bytes (SMS) but 123 UTF-8 bytes (would be
     * LMS under the old seeder rule). The shared policy must classify it as SMS, proving the seeder
     * and runtime no longer drift.
     */
    @DisplayName("EUC-KR↔UTF-8 경계 본문은 EUC-KR 기준(SMS)으로 일관 분류된다")
    @Test
    void resolve_eucKrVsUtf8Boundary_followsEucKr() {
        String content = "가".repeat(41);
        int eucKrBytes = content.getBytes(java.nio.charset.Charset.forName("EUC-KR")).length;
        int utf8Bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

        assertThat(eucKrBytes).isLessThanOrEqualTo(90); // EUC-KR rule ⇒ SMS
        assertThat(utf8Bytes).isGreaterThan(90);        // old UTF-8 rule would have said LMS
        assertThat(SmsChannelPolicy.resolve(content)).isEqualTo(SmsChannel.SMS);
    }
}
