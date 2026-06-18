package org.streamhub.api.v1.goods;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.goods.entity.GoodsItem;
import org.streamhub.api.v1.goods.repository.GoodsItemRepository;
import org.streamhub.api.v1.goods.inquiry.entity.AnswerStatus;
import org.streamhub.api.v1.goods.inquiry.entity.GoodsInquiry;
import org.streamhub.api.v1.goods.inquiry.repository.GoodsInquiryRepository;
import org.streamhub.api.v1.goods.review.entity.GoodsReview;
import org.streamhub.api.v1.goods.review.repository.GoodsReviewRepository;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.repository.MemberRepository;

/**
 * Seeds the goods feedback demo dataset (inquiries + reviews) on top of the goods items and
 * members produced by the earlier seeders. Runs after {@code DataInitializer} (@Order(1)),
 * {@code PortfolioSeeder} (@Order(2)) and {@code WorshipSeeder} (@Order(3)). Idempotent (skips
 * when both feedback tables already hold rows). The fixed-seed {@link Random} makes the dataset
 * <em>shape</em> (status mix, rating distribution, field values) reproducible across runs; the
 * absolute dates are <em>not</em> fixed — every row is anchored to {@link LocalDateTime#now()},
 * so the {@value #WINDOW_DAYS}-day window rolls forward to stay current. All values are
 * demo/fictional (PII guard).
 */
@Slf4j
@Component
@Order(10)
public class GoodsFeedbackSeeder implements CommandLineRunner {

    /** Baseline window: the most recent 120 days. */
    private static final int WINDOW_DAYS = 120;

    private static final long SEED = 910L;
    private static final int TARGET_INQUIRIES = 40;
    private static final int TARGET_REVIEWS = 60;

    private static final String[] FAKE_NAMES = {
            "홍길동", "김은혜", "이찬양", "박소망", "정믿음", "최사랑", "강하늘", "조은별",
            "윤지혜", "장다윗", "임예린", "한바울", "오누가", "서요한", "신마리"
    };

    private static final String[] INQUIRY_TITLES = {
            "사이즈 문의드려요", "배송 언제 되나요?", "재입고 예정인가요?", "색상 차이 있나요?",
            "교환 가능한가요?", "정품 맞나요?", "포장 상태 문의", "수량 추가 주문 가능할까요?",
            "세탁 방법 알려주세요", "선물 포장 되나요?"
    };

    private static final String[] INQUIRY_CONTENTS = {
            "구매 전에 자세한 정보가 궁금해서 문의드립니다. 빠른 답변 부탁드려요.",
            "주문하려고 하는데 확인 부탁드립니다.",
            "상품 페이지에 안내가 없어서 여쭤봅니다.",
            "이전에 받은 상품과 비교해서 궁금한 점이 있습니다.",
            "급하게 필요한 상품이라 답변 기다리겠습니다."
    };

    private static final String[] ANSWER_CONTENTS = {
            "안녕하세요. 문의 주셔서 감사합니다. 확인 후 안내드립니다.",
            "문의하신 내용은 상품 상세페이지를 참고 부탁드립니다. 추가 문의는 고객센터로 연락 주세요.",
            "현재 정상 배송 중이며 주문일 기준 2~3일 내 수령 가능합니다.",
            "재입고 일정은 확정되는 대로 알림 신청자분들께 안내드리겠습니다."
    };

    private static final String[] REVIEW_CONTENTS = {
            "음질이 좋아요. 잘 쓰고 있습니다.", "배송도 빠르고 포장도 꼼꼼했어요.",
            "선물용으로 샀는데 받는 분이 정말 좋아했어요.", "가격 대비 만족스럽습니다.",
            "디자인이 예쁘고 마감도 깔끔합니다.", "생각보다 크기가 조금 작아요. 그래도 만족합니다.",
            "재구매 의사 있습니다. 추천해요.", "교회 행사 때 단체로 구매했는데 다들 만족했어요.",
            "사진과 동일하고 품질 좋습니다.", "무난하게 잘 쓰고 있어요."
    };

    private final GoodsItemRepository goodsItemRepository;
    private final MemberRepository memberRepository;
    private final GoodsInquiryRepository goodsInquiryRepository;
    private final GoodsReviewRepository goodsReviewRepository;

    public GoodsFeedbackSeeder(
            GoodsItemRepository goodsItemRepository,
            MemberRepository memberRepository,
            GoodsInquiryRepository goodsInquiryRepository,
            GoodsReviewRepository goodsReviewRepository) {
        this.goodsItemRepository = goodsItemRepository;
        this.memberRepository = memberRepository;
        this.goodsInquiryRepository = goodsInquiryRepository;
        this.goodsReviewRepository = goodsReviewRepository;
    }

    @Override
    public void run(String... args) {
        if (goodsInquiryRepository.count() > 0 && goodsReviewRepository.count() > 0) {
            return;
        }
        List<GoodsItem> goodsItems = goodsItemRepository.findAll();
        List<Member> members = memberRepository.findAll();
        if (goodsItems.isEmpty() || members.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);

        seedInquiries(goodsItems, members, now, rnd);
        seedReviews(goodsItems, members, now, rnd);
    }

    private void seedInquiries(List<GoodsItem> goodsItems, List<Member> members,
                              LocalDateTime now, Random rnd) {
        List<GoodsInquiry> inquiries = new ArrayList<>();
        for (int i = 0; i < TARGET_INQUIRIES; i++) {
            GoodsItem item = goodsItems.get(rnd.nextInt(goodsItems.size()));
            boolean linkMember = rnd.nextInt(100) < 80;
            Member member = members.get(rnd.nextInt(members.size()));
            String memberName = linkMember ? member.getName() : FAKE_NAMES[rnd.nextInt(FAKE_NAMES.length)];
            LocalDateTime createdAt = distributedDateTime(now, rnd);
            boolean answered = rnd.nextInt(100) >= 55; // ~55% WAITING, rest ANSWERED

            inquiries.add(GoodsInquiry.builder()
                    .goodsItemId(item.getId())
                    .memberId(linkMember ? member.getId() : null)
                    .memberName(memberName)
                    .title(INQUIRY_TITLES[rnd.nextInt(INQUIRY_TITLES.length)])
                    .content(INQUIRY_CONTENTS[rnd.nextInt(INQUIRY_CONTENTS.length)])
                    .answerStatus(answered ? AnswerStatus.ANSWERED : AnswerStatus.WAITING)
                    .answerContent(answered ? ANSWER_CONTENTS[rnd.nextInt(ANSWER_CONTENTS.length)] : null)
                    .createdAt(createdAt)
                    .answeredAt(answered ? createdAt.plusDays(1 + rnd.nextInt(3)) : null)
                    .build());
        }
        goodsInquiryRepository.saveAll(inquiries);
        log.info("Seeded {} goods inquiries", inquiries.size());
    }

    private void seedReviews(List<GoodsItem> goodsItems, List<Member> members,
                            LocalDateTime now, Random rnd) {
        List<GoodsReview> reviews = new ArrayList<>();
        for (int i = 0; i < TARGET_REVIEWS; i++) {
            GoodsItem item = goodsItems.get(rnd.nextInt(goodsItems.size()));
            boolean linkMember = rnd.nextInt(100) < 85;
            Member member = members.get(rnd.nextInt(members.size()));
            String memberName = linkMember ? member.getName() : FAKE_NAMES[rnd.nextInt(FAKE_NAMES.length)];

            reviews.add(GoodsReview.builder()
                    .goodsItemId(item.getId())
                    .memberId(linkMember ? member.getId() : null)
                    .memberName(memberName)
                    .rating(skewedRating(rnd))
                    .content(REVIEW_CONTENTS[rnd.nextInt(REVIEW_CONTENTS.length)])
                    .displayYn(rnd.nextInt(100) < 85 ? "Y" : "N") // ~85% displayed
                    .createdAt(distributedDateTime(now, rnd))
                    .build());
        }
        goodsReviewRepository.saveAll(reviews);
        log.info("Seeded {} goods reviews", reviews.size());
    }

    /** Rating distribution skewed toward 4–5 stars (~60% 5, ~25% 4, rest 1–3). */
    private int skewedRating(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 60) {
            return 5;
        }
        if (r < 85) {
            return 4;
        }
        if (r < 95) {
            return 3;
        }
        return 1 + rnd.nextInt(2); // 1..2
    }

    /** Produces a {@link LocalDateTime} within the last {@link #WINDOW_DAYS} days. */
    private LocalDateTime distributedDateTime(LocalDateTime now, Random rnd) {
        int daysAgo = rnd.nextInt(WINDOW_DAYS + 1); // 0..120
        return now.minusDays(daysAgo)
                .withHour(9 + rnd.nextInt(12))
                .withMinute(rnd.nextInt(60))
                .withSecond(0)
                .withNano(0);
    }
}
