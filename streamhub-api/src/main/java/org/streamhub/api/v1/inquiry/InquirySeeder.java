package org.streamhub.api.v1.inquiry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.inquiry.entity.CustomerInquiry;
import org.streamhub.api.v1.inquiry.entity.InquiryCategory;
import org.streamhub.api.v1.inquiry.entity.InquiryStatus;
import org.streamhub.api.v1.inquiry.repository.CustomerInquiryRepository;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.repository.MemberRepository;

/**
 * Seeds the 1:1 customer inquiry demo dataset (고객 문의). Idempotent (skips when the
 * inquiry table already holds rows). The fixed-seed {@link Random} makes the dataset
 * <em>shape</em> (status mix, category spread, field values) reproducible; the absolute
 * dates are <em>not</em> fixed — every row is anchored to {@link LocalDateTime#now()}, so
 * the recent-60-day window rolls forward to stay current. Real members are linked when
 * present (id + name); otherwise fictional names stand in. All PII is virtual/masked.
 */
@Slf4j
@Component
@Order(14)
public class InquirySeeder implements CommandLineRunner {

    private static final long SEED = 914L;
    private static final int TARGET_INQUIRIES = 50;
    private static final int WINDOW_DAYS = 60;

    private static final String[] SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
    private static final String[] GIVEN_NAMES = {"민준", "서연", "도윤", "지우", "예준", "하은", "주원", "지호", "수아", "지민"};

    private static final InquiryCategory[] CATEGORIES = InquiryCategory.values();

    /** Category-specific title pool keyed by ordinal of {@link InquiryCategory}. */
    private static final String[][] TITLES = {
            {"계정 비밀번호 변경", "로그인이 안돼요", "이메일 인증 메일이 안와요", "회원 탈퇴 문의"},
            {"결제가 안돼요", "환불 요청합니다", "이중 결제된 것 같아요", "결제 영수증을 받고 싶어요"},
            {"배송 문의", "상품이 아직 안왔어요", "배송지 변경하고 싶어요", "송장 번호 확인 부탁드려요"},
            {"영상이 재생되지 않습니다", "라이브 화면이 멈춰요", "자막이 안나와요", "다시보기가 안돼요"},
            {"이용 문의드립니다", "기타 건의사항", "앱 사용법이 궁금해요", "공지사항 관련 문의"}
    };

    private static final String[] CONTENTS = {
            "안녕하세요. 문의 드립니다. 빠른 확인 부탁드립니다.",
            "이용 중 불편한 점이 있어 문의 남깁니다. 답변 기다리겠습니다.",
            "관련해서 자세한 안내가 필요합니다. 확인 후 회신 부탁드립니다.",
            "급한 건이라 빠른 처리 부탁드립니다. 감사합니다."
    };

    private static final String[] ANSWERS = {
            "안녕하세요. 문의 주셔서 감사합니다. 확인 결과 정상 처리되었습니다.",
            "불편을 드려 죄송합니다. 해당 내용 확인하여 조치하였습니다.",
            "문의하신 내용은 아래와 같이 안내드립니다. 추가 문의는 재문의 부탁드립니다.",
            "확인 결과 정상적으로 처리되었습니다. 이용에 참고 부탁드립니다."
    };

    private final CustomerInquiryRepository customerInquiryRepository;
    private final MemberRepository memberRepository;

    public InquirySeeder(CustomerInquiryRepository customerInquiryRepository,
                         MemberRepository memberRepository) {
        this.customerInquiryRepository = customerInquiryRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) {
        if (customerInquiryRepository.count() > 0) {
            return;
        }
        Random rnd = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();
        List<Member> members = memberRepository.findAll();

        List<CustomerInquiry> inquiries = new ArrayList<>();
        for (int i = 0; i < TARGET_INQUIRIES; i++) {
            InquiryCategory category = CATEGORIES[i % CATEGORIES.length];
            InquiryStatus status = resolveStatus(rnd);

            Long memberId = null;
            String memberName;
            if (!members.isEmpty() && rnd.nextInt(100) < 70) {
                Member member = members.get(rnd.nextInt(members.size()));
                memberId = member.getId();
                memberName = member.getName();
            } else {
                memberName = SURNAMES[rnd.nextInt(SURNAMES.length)]
                        + GIVEN_NAMES[rnd.nextInt(GIVEN_NAMES.length)];
            }

            LocalDateTime createdAt = now.minusDays(rnd.nextInt(WINDOW_DAYS + 1))
                    .withHour(9 + rnd.nextInt(12)).withMinute(rnd.nextInt(60)).withSecond(0).withNano(0);
            if (createdAt.isAfter(now)) {
                createdAt = now.minusHours(1 + rnd.nextInt(8)).withSecond(0).withNano(0);
            }

            String[] titlePool = TITLES[category.ordinal()];
            String title = titlePool[rnd.nextInt(titlePool.length)];

            boolean answered = status == InquiryStatus.ANSWERED || status == InquiryStatus.CLOSED;
            String answerContent = answered ? ANSWERS[rnd.nextInt(ANSWERS.length)] : null;
            LocalDateTime answeredAt = answered ? answeredDateTime(createdAt, now, rnd) : null;

            inquiries.add(CustomerInquiry.builder()
                    .memberId(memberId)
                    .memberName(memberName)
                    .category(category)
                    .title(title)
                    .content(CONTENTS[rnd.nextInt(CONTENTS.length)])
                    .status(status)
                    .answerContent(answerContent)
                    .createdAt(createdAt)
                    .answeredAt(answeredAt)
                    .build());
        }
        customerInquiryRepository.saveAll(inquiries);
        log.info("Seeded {} customer inquiries", inquiries.size());
    }

    /** Resolves the 40/45/15 OPEN/ANSWERED/CLOSED status mix. */
    private InquiryStatus resolveStatus(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 40) {
            return InquiryStatus.OPEN;
        }
        if (r < 85) {
            return InquiryStatus.ANSWERED;
        }
        return InquiryStatus.CLOSED;
    }

    /** A reply time after {@code createdAt} (0..3 days later) and never in the future. */
    private LocalDateTime answeredDateTime(LocalDateTime createdAt, LocalDateTime now, Random rnd) {
        LocalDateTime answeredAt = createdAt
                .plusDays(rnd.nextInt(4))
                .plusHours(1 + rnd.nextInt(12))
                .withSecond(0).withNano(0);
        return answeredAt.isAfter(now) ? now.withSecond(0).withNano(0) : answeredAt;
    }
}
