package org.streamhub.api.v1.campaign;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.campaign.entity.Campaign;
import org.streamhub.api.v1.campaign.entity.CampaignStatus;
import org.streamhub.api.v1.campaign.entity.CampaignType;
import org.streamhub.api.v1.campaign.repository.CampaignRepository;

/**
 * Seeds the campaign/event demo dataset (C-campaign). Idempotent (skips when the campaign
 * table already holds rows). The fixed-seed {@link Random} makes the dataset <em>shape</em>
 * (type/status mix, target amounts, linked goods) reproducible across runs; the absolute
 * dates are <em>not</em> fixed — every row is anchored to {@link LocalDateTime#now()}, so the
 * active window rolls forward to stay current relative to today. All values are demo/fictional.
 */
@Slf4j
@Component
@Order(16)
public class CampaignSeeder implements CommandLineRunner {

    private static final long SEED = 916L;
    private static final int TARGET_CAMPAIGNS = 20;

    private static final String[] TITLES = {
            "성탄절 특별헌금",
            "신간 찬양앨범 사전예약",
            "여름 성경학교 이벤트",
            "감사절 시즌 캠페인",
            "부활절 특별헌금",
            "신규 굿즈 출시 기념 이벤트",
            "맥추절 감사 캠페인",
            "겨울 사랑나눔 특별헌금",
            "봄 시즌 신상 사전예약",
            "창립기념 감사 이벤트"
    };

    private static final CampaignType[] TYPES = CampaignType.values();

    private final CampaignRepository campaignRepository;

    public CampaignSeeder(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    public void run(String... args) {
        if (campaignRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);

        List<Campaign> campaigns = new ArrayList<>();
        for (int i = 0; i < TARGET_CAMPAIGNS; i++) {
            CampaignType type = TYPES[i % TYPES.length];
            CampaignStatus status = resolveStatus(rnd);

            LocalDateTime startAt = now.minusDays(rnd.nextInt(30))
                    .withHour(9).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endAt = startAt.plusDays(14 + rnd.nextInt(45));

            Long targetAmount = type == CampaignType.SPECIAL_DONATION
                    ? (long) (5_000_000 + rnd.nextInt(46) * 1_000_000) // 5,000,000 .. 50,000,000
                    : null;

            String linkedGoodsIds = type == CampaignType.NEW_RELEASE || type == CampaignType.EVENT
                    ? buildLinkedGoodsIds(rnd)
                    : null;

            Campaign campaign = Campaign.builder()
                    .title(TITLES[i % TITLES.length] + " " + (i + 1) + "차")
                    .type(type)
                    .description("데모 캠페인 설명입니다. " + TITLES[i % TITLES.length]
                            + " 관련 안내와 참여 방법을 담은 가상 콘텐츠입니다.")
                    .bannerImageUrl("https://picsum.photos/seed/campaign" + (i + 1) + "/1000/400")
                    .linkedGoodsIds(linkedGoodsIds)
                    .targetAmount(targetAmount)
                    .startAt(startAt)
                    .endAt(endAt)
                    .status(status)
                    .createdAt(startAt)
                    .build();
            campaigns.add(campaign);
        }
        campaignRepository.saveAll(campaigns);
        log.info("Seeded {} campaigns", campaigns.size());
    }

    /** Status mix: ~15% DRAFT, ~65% ACTIVE, ~20% ENDED. */
    private CampaignStatus resolveStatus(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 15) {
            return CampaignStatus.DRAFT;
        }
        if (r < 80) {
            return CampaignStatus.ACTIVE;
        }
        return CampaignStatus.ENDED;
    }

    /** Builds a comma-separated list of 1..3 demo goods ids (e.g. {@code "1,2,3"}). */
    private String buildLinkedGoodsIds(Random rnd) {
        int count = 1 + rnd.nextInt(3);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(1 + rnd.nextInt(10));
        }
        return sb.toString();
    }
}
