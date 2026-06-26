package org.streamhub.api.base.external.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Real Kakao Local discovery provider — <b>live integration</b> against {@code dapi.kakao.com}.
 * Runs a radius keyword search ({@code search/keyword}, sort=distance) for "교회" around the
 * origin and returns the surrounding churches as {@link DiscoveredChurch}. Results are merged
 * into the DB nearby search by {@code ChurchService}, surfacing real churches that are not
 * registered in our DB. <b>Real-time only — nothing is persisted</b> (Kakao terms restrict
 * storing/redistributing POI data).
 *
 * <p>Active when {@code church.discovery.provider=kakao}; reuses the same Kakao REST key as the
 * geocode provider ({@code church.geocode.kakao-rest-key}). Kakao caps the radius at 20km and
 * returns at most 15 results/page; up to 3 pages (≈45) are collected.
 */
@Component
@ConditionalOnProperty(name = "church.discovery.provider", havingValue = "kakao")
public class KakaoChurchDiscoveryProvider implements ChurchDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(KakaoChurchDiscoveryProvider.class);

    private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String DEFAULT_KEYWORD = "교회";

    /**
     * Annex / office / facility tokens that the "교회" keyword sweeps in but which are not a church
     * a user would visit (parking lots, education halls, denomination HQs, memorial halls, …).
     * Matched as a substring of the place name. Chosen to be safe: each is highly unlikely to occur
     * inside a genuine church's own name (e.g. {@code 재단} is intentionally excluded from this list
     * because real church names like "…유지재단서대문교회" contain it).
     */
    private static final String[] EXCLUDE_TOKENS = {
        "주차", "교육관", "교육원", "총회본부", "본부", "연합회", "연회", "선교관",
        "봉사관", "기념관", "사택", "기도원", "보호센터", "센터", "빌딩", "정문", "입구",
    };
    /** Kakao radius hard cap (metres). */
    private static final int MAX_RADIUS_M = 20_000;
    /** Page size (Kakao max 15) and page cap (15×3 ≈ 45 results). */
    private static final int PAGE_SIZE = 15;
    private static final int MAX_PAGES = 3;
    /** Connect/read timeouts (ms) — a slow Kakao must not stall the nearby response (merge is best-effort). */
    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private static final int READ_TIMEOUT_MS = 2_000;

    private final String kakaoRestKey;
    private final RestClient restClient;

    public KakaoChurchDiscoveryProvider(
            @Value("${church.geocode.kakao-rest-key:}") String kakaoRestKey,
            RestClient.Builder restClientBuilder) {
        this.kakaoRestKey = kakaoRestKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = restClientBuilder.requestFactory(factory).build();
    }

    /**
     * Live Kakao Local lookup, cached short-term in Redis (60s, {@code churchDiscovery} cache).
     * The key snaps the origin to a ~1.1km grid ({@code round(coord*100)}) so repeated searches of
     * the same neighbourhood — page reloads, several users in one area — reuse a single upstream
     * call instead of hammering Kakao on every request. Exact per-user distances are recomputed
     * downstream in {@code ChurchService}, so the coarse grid does not affect result accuracy.
     * Empty results are not cached, so a transient miss is retried rather than pinned for the TTL.
     */
    @Override
    @Cacheable(
            cacheNames = "churchDiscovery",
            key = "T(java.lang.Math).round(#lat * 100) + ':' + T(java.lang.Math).round(#lng * 100)"
                    + " + ':' + T(java.lang.Math).round(#radiusKm)"
                    + " + ':' + (#keyword == null ? '' : #keyword.trim())",
            unless = "#result.isEmpty()")
    public List<DiscoveredChurch> search(double lat, double lng, double radiusKm, String keyword) {
        if (!StringUtils.hasText(kakaoRestKey)) {
            throw new ApiException(ResultCode.INTERNAL_ERROR,
                    "Kakao 키가 설정되지 않았습니다 (church.geocode.kakao-rest-key)");
        }
        log.info("Kakao discovery LIVE call (cache miss): lat={}, lng={}, radiusKm={}, keyword={}",
                lat, lng, radiusKm, keyword);
        String query = StringUtils.hasText(keyword) ? keyword.trim() : DEFAULT_KEYWORD;
        int radiusM = Math.min((int) Math.round(radiusKm * 1000), MAX_RADIUS_M);

        List<DiscoveredChurch> results = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode body = call(query, lat, lng, radiusM, page);
            JsonNode documents = body.get("documents");
            if (documents == null || !documents.isArray() || documents.isEmpty()) {
                break;
            }
            for (JsonNode doc : documents) {
                DiscoveredChurch church = toChurch(doc);
                if (church != null) {
                    results.add(church);
                }
            }
            JsonNode meta = body.get("meta");
            boolean isEnd = meta == null || meta.path("is_end").asBoolean(true);
            if (isEnd) {
                break;
            }
        }
        return results;
    }

    private JsonNode call(String query, double lat, double lng, int radiusM, int page) {
        URI uri = UriComponentsBuilder.fromUriString(KEYWORD_URL)
                .queryParam("query", query)
                .queryParam("x", lng) // Kakao x = longitude
                .queryParam("y", lat) // Kakao y = latitude
                .queryParam("radius", radiusM)
                .queryParam("size", PAGE_SIZE)
                .queryParam("page", page)
                .queryParam("sort", "distance")
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
            throw new ApiException(ResultCode.INTERNAL_ERROR, "Kakao 장소검색 API 호출 실패");
        }
        return body;
    }

    /** Maps one Kakao document to a church, or null when it is not a church / lacks coordinates. */
    private DiscoveredChurch toChurch(JsonNode doc) {
        String name = text(doc, "place_name");
        String category = text(doc, "category_name");
        if (!isRelevantChurch(name, category)) {
            return null;
        }
        String x = text(doc, "x");
        String y = text(doc, "y");
        if (x == null || y == null) {
            return null;
        }
        String address = firstNonBlank(text(doc, "road_address_name"), text(doc, "address_name"));
        return new DiscoveredChurch(
                text(doc, "id"),
                name,
                Double.parseDouble(y),
                Double.parseDouble(x),
                address,
                text(doc, "phone"),
                shortCategory(category),
                text(doc, "place_url"));
    }

    /**
     * Decides whether a Kakao place is a real, visitable church. It must look church-like (name or
     * category mentions 교회/성당/채플) and must NOT be an annex/office/parking facility
     * (see {@link #EXCLUDE_TOKENS}). Package-private and static so it can be unit-tested without an
     * HTTP call.
     */
    static boolean isRelevantChurch(String name, String category) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String haystack = name + " " + (category == null ? "" : category);
        boolean churchLike = haystack.contains("교회") || haystack.contains("성당") || haystack.contains("채플");
        if (!churchLike) {
            return false;
        }
        for (String token : EXCLUDE_TOKENS) {
            if (name.contains(token)) {
                return false;
            }
        }
        return true;
    }

    /** Last segment of a {@code "종교 > 천주교 > 성당"} category path. */
    private String shortCategory(String category) {
        if (category == null) {
            return null;
        }
        int idx = category.lastIndexOf('>');
        return (idx >= 0 ? category.substring(idx + 1) : category).trim();
    }

    private String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : (StringUtils.hasText(b) ? b : null);
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }
}
