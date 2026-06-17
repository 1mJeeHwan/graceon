package org.streamhub.api.base.external.geocode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Real-key injection point for Kakao Local geocoding (address → coordinate). <b>Stub only</b>:
 * this spec makes no external call. The REST key wiring is in place; the actual HTTP call is
 * intentionally unimplemented and the provider is inactive by default.
 *
 * <p>Activate by setting {@code church.geocode.provider=kakao} and supplying
 * {@code church.geocode.kakao-rest-key}; the real call would send a {@code KakaoAK} header to
 * the Local API and return {@code source="KAKAO"}, {@code demo=false}.
 */
@Component
@ConditionalOnProperty(name = "church.geocode.provider", havingValue = "kakao")
public class KakaoGeocodeProvider implements GeocodeProvider {

    /** Real REST key injection point (empty by default = not wired). */
    private final String kakaoRestKey;

    public KakaoGeocodeProvider(@Value("${church.geocode.kakao-rest-key:}") String kakaoRestKey) {
        this.kakaoRestKey = kakaoRestKey;
    }

    @Override
    public GeocodeResult geocode(String address) {
        // Real-key injection seam: the Kakao Local HTTP call is intentionally not implemented
        // in this spec. Enabling this provider without wiring the call is a configuration error.
        throw new ApiException(ResultCode.INTERNAL_ERROR,
                "KakaoGeocodeProvider는 실키 주입 스텁입니다 (church.geocode.kakao-rest-key 미구현)");
    }
}
