package org.streamhub.api.base.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("3.172.10.20"); // CloudFront edge
        if (forwardedFor != null) {
            req.addHeader("X-Forwarded-For", forwardedFor);
        }
        return req;
    }

    @Test
    void picksRightmostEntryAppendedByCloudFront() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        // Normal browser: CloudFront appends the viewer as the only entry.
        assertThat(resolver.resolve(request("211.34.56.78"))).isEqualTo("211.34.56.78");
        // Spoof attempt: client-sent junk sits left of the CloudFront-appended viewer.
        assertThat(resolver.resolve(request("6.6.6.6, 211.34.56.78"))).isEqualTo("211.34.56.78");
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderMissingOrProxyDisabled() {
        assertThat(new ClientIpResolver(1).resolve(request(null))).isEqualTo("3.172.10.20");
        assertThat(new ClientIpResolver(0).resolve(request("6.6.6.6"))).isEqualTo("3.172.10.20");
    }
}
