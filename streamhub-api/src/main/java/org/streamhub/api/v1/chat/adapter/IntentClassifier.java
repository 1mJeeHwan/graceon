package org.streamhub.api.v1.chat.adapter;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.chat.entity.ChatIntent;

/**
 * Rule-based intent classifier (C5). Lower-cases the message and matches keyword sets in
 * priority order: {@code FAQ} → {@code ORDER_LOOKUP} → {@code PRODUCT_INQUIRY} → {@code FALLBACK}.
 * FAQ is checked first so a specific phrase like "배송비" wins over the broader order keyword "배송".
 *
 * <p>This is the core branch of the rule chatbot, so it is covered by a table-based unit test.
 */
@Component
public class IntentClassifier {

    private static final List<String> ORDER_KEYWORDS =
            List.of("주문", "배송", "조회", "택배", "운송장", "order", "tracking");
    private static final List<String> PRODUCT_KEYWORDS =
            List.of("상품", "가격", "재고", "구매", "앨범", "굿즈", "product", "price", "stock");
    private static final List<String> FAQ_KEYWORDS =
            List.of("배송비", "환불", "교환", "반품", "회원", "예배", "시간", "포인트", "쿠폰", "faq");

    /** Classifies a user message into a {@link ChatIntent}. Null/blank ⇒ {@code FALLBACK}. */
    public ChatIntent classify(String message) {
        if (message == null || message.isBlank()) {
            return ChatIntent.FALLBACK;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (containsAny(lower, FAQ_KEYWORDS)) {
            return ChatIntent.FAQ;
        }
        if (containsAny(lower, ORDER_KEYWORDS)) {
            return ChatIntent.ORDER_LOOKUP;
        }
        if (containsAny(lower, PRODUCT_KEYWORDS)) {
            return ChatIntent.PRODUCT_INQUIRY;
        }
        return ChatIntent.FALLBACK;
    }

    private boolean containsAny(String lower, List<String> keywords) {
        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
