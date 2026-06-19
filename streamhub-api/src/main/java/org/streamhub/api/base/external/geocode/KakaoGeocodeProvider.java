package org.streamhub.api.base.external.geocode;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Real Kakao Local geocoding provider (address → coordinate) — <b>live integration</b> against
 * {@code dapi.kakao.com}. Sends a {@code KakaoAK} authorization header to the Local API and
 * returns {@code source="KAKAO"}, {@code demo=false}, so churches registered with only an address
 * get real WGS84 coordinates and the "내 주변 교회" distance ranking reflects reality.
 *
 * <p>Active when {@code church.geocode.provider=kakao}; the REST key is injected via
 * {@code church.geocode.kakao-rest-key} ({@code CHURCH_GEOCODE_KAKAO_REST_KEY}, not committed —
 * public repo). Resolution strategy: the address-search endpoint first ({@code search/address})
 * and, when it yields nothing (partial/old/lot addresses), a keyword-search fallback
 * ({@code search/keyword}) to maximise the hit rate. A blank key or an unresolvable address
 * raises a clear {@link ApiException} rather than silently producing demo coordinates.
 */
@Component
@ConditionalOnProperty(name = "church.geocode.provider", havingValue = "kakao")
public class KakaoGeocodeProvider implements GeocodeProvider {

    /** Source marker for real Kakao coordinates (drives the demo badge: real = no badge). */
    private static final String SOURCE_KAKAO = "KAKAO";

    private static final String ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    private final String kakaoRestKey;
    private final RestClient restClient;

    public KakaoGeocodeProvider(
            @Value("${church.geocode.kakao-rest-key:}") String kakaoRestKey,
            RestClient.Builder restClientBuilder) {
        this.kakaoRestKey = kakaoRestKey;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public GeocodeResult geocode(String address) {
        if (!StringUtils.hasText(kakaoRestKey)) {
            throw new ApiException(ResultCode.INTERNAL_ERROR,
                    "Kakao 지오코딩 키가 설정되지 않았습니다 (church.geocode.kakao-rest-key)");
        }
        String query = address == null ? "" : address.trim();
        if (!StringUtils.hasText(query)) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "지오코딩할 주소가 비어 있습니다");
        }

        // Road/lot address lookup first; fall back to keyword search when it returns nothing.
        JsonNode hit = firstDocument(ADDRESS_URL, query);
        if (hit == null) {
            hit = firstDocument(KEYWORD_URL, query);
        }
        if (hit == null) {
            throw new ApiException(ResultCode.INVALID_PARAMETER,
                    "주소를 좌표로 변환할 수 없습니다: " + query);
        }

        // Kakao returns x = longitude, y = latitude (as strings).
        double longitude = hit.get("x").asDouble();
        double latitude = hit.get("y").asDouble();
        return new GeocodeResult(round6(latitude), round6(longitude), SOURCE_KAKAO, false);
    }

    /** Calls a Local search endpoint and returns the first {@code documents} entry, or null. */
    private JsonNode firstDocument(String baseUrl, String query) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("query", query)
                .queryParam("size", 1)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        ResponseEntity<JsonNode> response = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestKey)
                .exchange((req, res) -> ResponseEntity
                        .status(res.getStatusCode())
                        .body(res.bodyTo(JsonNode.class)));

        JsonNode body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null) {
            throw new ApiException(ResultCode.INTERNAL_ERROR, "Kakao 지오코딩 API 호출 실패");
        }
        JsonNode documents = body.get("documents");
        if (documents == null || !documents.isArray() || documents.isEmpty()) {
            return null;
        }
        JsonNode first = documents.get(0);
        return (first.hasNonNull("x") && first.hasNonNull("y")) ? first : null;
    }

    private double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
