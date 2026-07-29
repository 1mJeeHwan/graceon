package org.streamhub.api.base.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Guards the masking applied to the public read-only demo account. The point of each case is that
 * the output stays recognizable to an operator while carrying no harvestable identifier.
 */
class PiiMaskerTest {

    @Test
    void email_keepsDomain_hidesLocalPart() {
        assertThat(PiiMasker.maskEmail("hong@example.com")).isEqualTo("ho**@example.com");
        // Short local parts must not leak by being too short to mask.
        assertThat(PiiMasker.maskEmail("a@example.com")).isEqualTo("**@example.com");
        // Not an address at all — still must not pass through untouched.
        assertThat(PiiMasker.maskEmail("plainstring")).isEqualTo("pl*********");
        assertThat(PiiMasker.maskEmail(null)).isNull();
    }

    @Test
    void phone_keepsCarrierPrefixAndLastFour() {
        assertThat(PiiMasker.maskPhone("010-1234-5678")).isEqualTo("010-****-5678");
        assertThat(PiiMasker.maskPhone("01012345678")).isEqualTo("010-****-5678");
        assertThat(PiiMasker.maskPhone("123")).isEqualTo("***");
        assertThat(PiiMasker.maskPhone("")).isEmpty();
    }

    @Test
    void ip_keepsFirstTwoOctets() {
        assertThat(PiiMasker.maskIp("211.34.56.78")).isEqualTo("211.34.*.*");
        assertThat(PiiMasker.maskIp("2001:db8:85a3::8a2e")).isEqualTo("2001:db8:*:*");
        assertThat(PiiMasker.maskIp("garbage")).isEqualTo("*.*");
    }

    @Test
    void loginId_masksBothEmailAndPlainIdentifiers() {
        // Failed sign-in rows carry whatever the caller typed — often a real member's address.
        assertThat(PiiMasker.maskLoginId("victim@example.com")).isEqualTo("vi****@example.com");
        assertThat(PiiMasker.maskLoginId("admin")).isEqualTo("ad***");
    }
}
