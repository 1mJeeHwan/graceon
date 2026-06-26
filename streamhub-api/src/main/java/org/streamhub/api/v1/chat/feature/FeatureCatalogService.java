package org.streamhub.api.v1.chat.feature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads the StreamHub feature catalog ({@code resources/feature-catalog.json}) once at startup and
 * serves it to the chatbot as a small, deterministic knowledge base — "이 기능이 있는지" and
 * "어떻게 쓰는지" answers. The dataset is ~30 fixed entries, so a keyword scan is enough; no search
 * engine (OpenSearch/Lucene) or external call is involved.
 *
 * <p>Both the rule provider and the LLM tool layer query this service, so feature answers are
 * grounded in the same source (no hallucinated features).
 */
@Service
public class FeatureCatalogService {

    private final List<FeatureInfo> all;
    private final Map<String, FeatureInfo> byId;

    public FeatureCatalogService(ObjectMapper objectMapper) {
        // The public chatbot must never surface admin-only features (or their admin console paths),
        // so admin-audience entries are dropped up front: they are invisible to search, the overview
        // and id lookup alike. This is the single chokepoint that enforces "no admin route leaks".
        this.all = load(objectMapper).stream().filter(FeatureInfo::isUserFacing).toList();
        Map<String, FeatureInfo> index = new LinkedHashMap<>();
        for (FeatureInfo f : all) {
            index.put(f.id(), f);
        }
        this.byId = index;
    }

    private List<FeatureInfo> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("feature-catalog.json").getInputStream()) {
            CatalogFile file = objectMapper.readValue(in, CatalogFile.class);
            return file.features() == null ? List.of() : List.copyOf(file.features());
        } catch (IOException e) {
            throw new IllegalStateException("feature-catalog.json 로드 실패", e);
        }
    }

    /** All catalog entries (immutable). */
    public List<FeatureInfo> all() {
        return all;
    }

    /** Looks up a feature by its slug id, or {@code null} if absent. */
    public FeatureInfo get(String id) {
        return id == null ? null : byId.get(id.trim());
    }

    /**
     * Keyword search over title/summary/howTo/domain/id. The query is split into tokens; each
     * feature is scored by how many distinct tokens it contains, and the top {@code limit} are
     * returned (highest score first, then catalog order). A blank query returns the first
     * {@code limit} entries; a query that matches nothing returns an empty list (so the caller can
     * honestly answer "그런 기능은 없습니다").
     *
     * @param query user keywords (e.g. "주문 환불", "쿠폰", "포인트 적립")
     * @param limit max results (clamped to ≥1)
     * @return matching features, best first
     */
    public List<FeatureInfo> search(String query, int limit) {
        int cap = Math.max(1, limit);
        if (query == null || query.isBlank()) {
            return all.stream().limit(cap).toList();
        }
        String[] tokens = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<Scored> scored = new ArrayList<>();
        for (FeatureInfo f : all) {
            String primary = primaryHaystack(f);     // id + title + keywords (the intended hooks)
            String secondary = secondaryHaystack(f);  // summary + howTo + domain (incidental mentions)
            int score = 0;
            for (String token : tokens) {
                if (token.isBlank()) {
                    continue;
                }
                // Match the raw token and a Korean-particle-stripped variant ("마케팅은" → "마케팅",
                // "포인트는" → "포인트"), so a natural question with an attached josa still matches.
                String bare = stripJosa(token);
                // A hit on the title/keywords is worth more than an incidental how-to mention, so a
                // feature whose name IS the query ("마이페이지") outranks one that only references it
                // in passing ("…는 마이페이지에서 확인합니다").
                if (primary.contains(token) || primary.contains(bare)) {
                    score += 2;
                } else if (secondary.contains(token) || secondary.contains(bare)) {
                    score += 1;
                }
            }
            if (score > 0) {
                scored.add(new Scored(f, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        return scored.stream().limit(cap).map(Scored::feature).toList();
    }

    /** Common trailing Korean particles (josa), longest first so "에서/으로" strip before "에/로". */
    private static final List<String> JOSA = List.of(
            "에서", "으로", "에게", "한테", "까지", "부터", "보다", "처럼", "은", "는", "이", "가",
            "을", "를", "과", "와", "도", "만", "의", "에", "로", "랑");

    /** Strips one trailing particle from a token when ≥2 chars remain ("이벤트는" → "이벤트"). */
    private String stripJosa(String token) {
        for (String josa : JOSA) {
            if (token.length() > josa.length() + 1 && token.endsWith(josa)) {
                return token.substring(0, token.length() - josa.length());
            }
        }
        return token;
    }

    /** The intended search hooks: id, title and the curated user-phrasing keywords. */
    private String primaryHaystack(FeatureInfo f) {
        return (nullToEmpty(f.id()) + ' ' + nullToEmpty(f.title()) + ' ' + nullToEmpty(f.keywords()))
                .toLowerCase(Locale.ROOT);
    }

    /** Incidental text — a match here is weaker than a title/keyword hit. */
    private String secondaryHaystack(FeatureInfo f) {
        return (nullToEmpty(f.summary()) + ' ' + nullToEmpty(f.howTo()) + ' ' + nullToEmpty(f.domain()))
                .toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private record Scored(FeatureInfo feature, int score) {
    }

    /** Root shape of {@code feature-catalog.json} ({@code _comment} ignored). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CatalogFile(List<FeatureInfo> features) {
    }
}
