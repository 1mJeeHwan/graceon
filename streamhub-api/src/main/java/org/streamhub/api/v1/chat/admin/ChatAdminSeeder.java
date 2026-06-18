package org.streamhub.api.v1.chat.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.chat.entity.ChatIntent;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;
import org.streamhub.api.v1.chat.entity.ChatSession;
import org.streamhub.api.v1.chat.repository.ChatMessageRepository;
import org.streamhub.api.v1.chat.repository.ChatSessionRepository;

/**
 * Seeds the chatbot demo dataset (C5) so the admin console has conversations to show. The
 * public {@code ChatService} only persists chat on live use, so the tables are empty at boot.
 * Idempotent (skips when the session table already holds rows). The fixed-seed {@link Random}
 * makes the dataset <em>shape</em> reproducible; timestamps are anchored to {@link LocalDateTime#now()}
 * so the conversations stay recent. Some sessions deliberately end on a USER turn (unanswered)
 * to exercise the operator queue. All values are demo/fictional (PII guard).
 */
@Slf4j
@Component
@Order(19)
public class ChatAdminSeeder implements CommandLineRunner {

    private static final long SEED = 919L;
    private static final int TARGET_SESSIONS = 25;

    /** FAQ-style user openers paired with an intent and a canned bot answer. */
    private static final Object[][] FAQS = {
            {"주문 조회하고 싶어요", ChatIntent.ORDER_LOOKUP,
                    "주문번호와 주문자 성함을 알려주시면 조회를 도와드리겠습니다."},
            {"배송이 안와요", ChatIntent.ORDER_LOOKUP,
                    "불편을 드려 죄송합니다. 주문번호를 알려주시면 배송 상태를 확인해 드리겠습니다."},
            {"환불 가능한가요?", ChatIntent.FAQ,
                    "상품 수령 후 7일 이내 미개봉 상품은 환불 가능합니다. 마이페이지에서 신청해 주세요."},
            {"배송비는 얼마인가요?", ChatIntent.FAQ,
                    "기본 배송비는 3,000원이며 30,000원 이상 구매 시 무료배송입니다."},
            {"이 찬양앨범 재고 있나요?", ChatIntent.PRODUCT_INQUIRY,
                    "현재 재고가 충분합니다. 상품 상세페이지에서 바로 구매하실 수 있습니다."},
            {"회원 등급 혜택이 궁금해요", ChatIntent.FAQ,
                    "등급별로 적립률과 전용 쿠폰이 다르게 제공됩니다. 마이페이지에서 확인하실 수 있습니다."},
            {"교환하려면 어떻게 하나요?", ChatIntent.FAQ,
                    "마이페이지 > 주문내역에서 교환 신청이 가능하며, 회수 후 재배송됩니다."},
            {"성경책 어떤 종류가 있나요?", ChatIntent.PRODUCT_INQUIRY,
                    "개역개정, 새번역 등 다양한 판형을 준비하고 있습니다. 도서 카테고리를 확인해 주세요."},
            {"쿠폰은 어디서 등록하나요?", ChatIntent.FAQ,
                    "마이페이지 > 쿠폰함에서 쿠폰 코드를 입력해 등록하실 수 있습니다."},
            {"안녕하세요", ChatIntent.FALLBACK,
                    "안녕하세요! 무엇을 도와드릴까요? 주문/배송/환불 관련 문의를 입력해 주세요."},
    };

    /** Follow-up user turns to lengthen some threads. */
    private static final String[] USER_FOLLOWUPS = {
            "감사합니다, 확인해볼게요",
            "그럼 언제쯤 받을 수 있나요?",
            "혹시 다른 방법은 없을까요?",
            "네 알겠습니다",
            "조금 더 자세히 알려주세요",
    };

    /** Follow-up bot turns to lengthen some threads. */
    private static final String[] BOT_FOLLOWUPS = {
            "추가로 궁금하신 점이 있으면 언제든 문의해 주세요.",
            "영업일 기준 2~3일 내 수령 가능하십니다.",
            "도움이 필요하시면 고객센터로도 연결해 드리겠습니다.",
            "확인 후 빠르게 처리해 드리겠습니다.",
            "말씀해 주신 내용 잘 접수했습니다.",
    };

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatAdminSeeder(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public void run(String... args) {
        // Seed the demo console only when chat history is sparse. Real widget usage may
        // leave a handful of live sessions; we still want a populated admin console for
        // the demo, so we top up rather than skip on the first stray session.
        if (chatSessionRepository.count() >= 10) {
            return;
        }
        Random rnd = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();

        int totalMessages = 0;
        for (int i = 0; i < TARGET_SESSIONS; i++) {
            LocalDateTime sessionStart = now.minusDays(rnd.nextInt(30)).minusMinutes(rnd.nextInt(720));
            ChatSession session = chatSessionRepository.save(ChatSession.builder()
                    .sessionKey(UUID.nameUUIDFromBytes(("chat-seed-" + i).getBytes()).toString())
                    .memberId(rnd.nextInt(100) < 60 ? (long) (1 + rnd.nextInt(20)) : null)
                    .provider("RULE")
                    .createdAt(sessionStart)
                    .build());
            totalMessages += seedMessages(session.getId(), sessionStart, rnd);
        }
        log.info("Seeded {} chat sessions, {} messages", TARGET_SESSIONS, totalMessages);
    }

    /** Seeds 2..6 alternating USER/BOT messages; ~30% of threads end on a USER turn (unanswered). */
    private int seedMessages(Long sessionId, LocalDateTime sessionStart, Random rnd) {
        Object[] faq = FAQS[rnd.nextInt(FAQS.length)];
        int turns = 2 + rnd.nextInt(5); // 2..6
        boolean unanswered = rnd.nextInt(100) < 30;
        if (unanswered && turns % 2 == 0) {
            turns--; // odd turn count ends on a USER message
        }

        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime at = sessionStart;
        for (int t = 0; t < turns; t++) {
            at = at.plusMinutes(1 + rnd.nextInt(5));
            if (t % 2 == 0) {
                String content = t == 0
                        ? (String) faq[0]
                        : USER_FOLLOWUPS[rnd.nextInt(USER_FOLLOWUPS.length)];
                messages.add(ChatMessage.builder()
                        .sessionId(sessionId)
                        .role(ChatRole.USER)
                        .content(content)
                        .createdAt(at)
                        .build());
            } else {
                String content = t == 1
                        ? (String) faq[2]
                        : BOT_FOLLOWUPS[rnd.nextInt(BOT_FOLLOWUPS.length)];
                messages.add(ChatMessage.builder()
                        .sessionId(sessionId)
                        .role(ChatRole.BOT)
                        .intent(t == 1 ? (ChatIntent) faq[1] : ChatIntent.FALLBACK)
                        .content(content)
                        .createdAt(at)
                        .build());
            }
        }
        chatMessageRepository.saveAll(messages);
        return messages.size();
    }
}
