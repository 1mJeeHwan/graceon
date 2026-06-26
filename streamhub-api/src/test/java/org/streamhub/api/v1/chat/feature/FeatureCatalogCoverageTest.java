package org.streamhub.api.v1.chat.feature;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.streamhub.api.v1.chat.adapter.IntentClassifier;
import org.streamhub.api.v1.chat.entity.ChatIntent;

/**
 * Coverage + safety guard for the chatbot's question classification (C5). The public chatbot must
 * (a) reach every USER-facing feature by a representative user-phrased question, (b) resolve every
 * recommended-question chip, and (c) never surface an admin-only feature or an admin-console route.
 */
class FeatureCatalogCoverageTest {

    private final FeatureCatalogService catalog = new FeatureCatalogService(new ObjectMapper());
    private final IntentClassifier intentClassifier = new IntentClassifier();

    /** Admin-only catalog ids — must be invisible to the public chatbot. */
    private static final List<String> ADMIN_IDS = List.of(
            "dashboard", "members", "action-log", "sms", "payment-seam", "subscription-plans",
            "subscription-calendar", "goods-category", "goods-stock", "visits", "content-stats",
            "boards", "banners", "notifications");

    /** Admin-console routes that must never appear in a public chatbot answer. */
    private static final List<String> ADMIN_ROUTES = List.of(
            "/dashboard", "/member", "/action-log", "/sms", "/payment", "/subscription-plan",
            "/billing-calendar", "/goods/category", "/goods/stock", "/visits", "/content/stats",
            "/boards", "/banners", "/notifications");

    /** Representative user question → the user-facing feature id it must reach. */
    private static final Map<String, String> QUESTION_TO_FEATURE = buildQuestionMap();

    private static Map<String, String> buildQuestionMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("예배 영상 어디서 봐?", "contents");
        m.put("내 주변 교회 찾기 지도", "churches");
        m.put("음반 미리듣기 어떻게?", "albums");
        m.put("오프라인 매장 찾기", "store");
        m.put("새가족 예배 신청하고 싶어", "worship");
        m.put("챗봇 도우미 사용법", "chat-bot");
        m.put("정기후원 구독 현황", "subscription");
        m.put("후원 헌금 하고 싶어", "donation");
        m.put("주문 배송 조회", "orders");
        m.put("굿즈샵 상품 구경", "goods");
        m.put("굿즈 문의 남기고 싶어", "goods-inquiry");
        m.put("상품 후기 평점", "goods-review");
        m.put("쿠폰함 할인 쿠폰", "coupons");
        m.put("포인트 적립 어떻게?", "points");
        m.put("공지 나눔 기도제목 소식", "posts");
        m.put("1:1 문의 고객센터", "inquiry");
        m.put("진행 중인 이벤트 있어?", "campaigns");
        m.put("마이페이지 뭐가 있어?", "mypage");
        m.put("찜한 곡 내 재생목록", "favorites");
        m.put("내 시청 기록 이어보기", "watch-history");
        m.put("통합검색 어떻게 해?", "search");
        m.put("회원가입 로그인 본인인증", "signup");
        return m;
    }

    @Test
    void everyUserFacingFeatureIsReachableByAnExpectedQuestion() {
        // The table must cover exactly the user-visible catalog (admin features excluded).
        assertThat(QUESTION_TO_FEATURE.values())
                .containsExactlyInAnyOrderElementsOf(catalog.all().stream().map(FeatureInfo::id).toList());

        QUESTION_TO_FEATURE.forEach((question, expectedId) -> {
            List<FeatureInfo> hits = catalog.search(question, 5);
            assertThat(hits)
                    .as("question \"%s\" should reach feature \"%s\"", question, expectedId)
                    .anyMatch(f -> f.id().equals(expectedId));
        });
    }

    @Test
    void adminFeaturesAreHiddenFromThePublicChatbot() {
        for (String adminId : ADMIN_IDS) {
            assertThat(catalog.get(adminId))
                    .as("admin feature \"%s\" must not be resolvable by the public chatbot", adminId)
                    .isNull();
        }
        assertThat(catalog.all().stream().map(FeatureInfo::id))
                .as("catalog exposed to the chatbot must contain no admin feature")
                .doesNotContainAnyElementsOf(ADMIN_IDS);
        // Every visible feature is genuinely user-facing.
        assertThat(catalog.all()).allMatch(FeatureInfo::isUserFacing);
    }

    @Test
    void noAdminConsoleRouteIsExposed() {
        // No visible feature deep-links into the admin console.
        assertThat(catalog.all().stream().map(FeatureInfo::href).toList())
                .as("no user-facing feature may point at an admin-console route")
                .doesNotContainAnyElementsOf(ADMIN_ROUTES);
    }

    @Test
    void everyRecommendedQuestionResolves() {
        List<String> featureChips = List.of(
                "교회찾기 기능", "음반 미리듣기 어떻게?", "이벤트 기능 있어?", "마이페이지 뭐가 있어?");
        for (String chip : featureChips) {
            assertThat(catalog.search(chip, 5))
                    .as("feature chip \"%s\" must match a catalog feature (not '찾지 못함')", chip)
                    .isNotEmpty();
        }

        Map<String, ChatIntent> intentChips = new LinkedHashMap<>();
        intentChips.put("어떤 기능이 있나요?", ChatIntent.FEATURE_GUIDE);
        intentChips.put("주문 배송 조회", ChatIntent.ORDER_LOOKUP);
        intentChips.put("상품 재고 문의", ChatIntent.PRODUCT_INQUIRY);
        intentChips.put("주문 조회하고 싶어요", ChatIntent.ORDER_LOOKUP);
        intentChips.forEach((chip, expected) ->
                assertThat(intentClassifier.classify(chip))
                        .as("intent chip \"%s\" should classify as %s", chip, expected)
                        .isEqualTo(expected));
    }
}
