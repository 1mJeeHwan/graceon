package org.streamhub.api.v1.worship.adapter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default postcode provider (demo/test mode). Performs no external call — it only trims the
 * caller-supplied values and passes them through. The public form's Daum/Kakao postcode
 * embed (no app key required) already fills {@code zipcode}/{@code addr1}; this seam exists
 * so a real server-side address-enrichment provider can be injected later via
 * {@code app.worship.postcode.provider=kakao} without touching {@code WorshipService}.
 */
@Component
@ConditionalOnProperty(name = "app.worship.postcode.provider", havingValue = "mock", matchIfMissing = true)
public class MockPostcodeProvider implements PostcodeProvider {

    @Override
    public PostcodeResult resolve(String zipcode, String addr1) {
        return new PostcodeResult(trimToNull(zipcode), trimToNull(addr1));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
