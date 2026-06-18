package org.streamhub.api.v1.banner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.banner.entity.Banner;
import org.streamhub.api.v1.banner.entity.BannerDevice;
import org.streamhub.api.v1.banner.entity.BannerPosition;
import org.streamhub.api.v1.banner.repository.BannerRepository;

/**
 * Seeds the front-banner demo dataset. Idempotent (skips when the banner table already holds
 * rows). The fixed-seed {@link Random} makes the dataset <em>shape</em> (position/device mix,
 * visibility, ordering) reproducible across runs; the absolute dates are <em>not</em> fixed —
 * every row is anchored to {@link LocalDateTime#now()}, so some windows are active and some
 * expired relative to today. All values are demo/fictional (image URLs are placeholders).
 */
@Slf4j
@Component
@Order(15)
public class BannerSeeder implements CommandLineRunner {

    private static final long SEED = 915L;
    private static final int TARGET_BANNERS = 24;

    private static final String[] TITLES = {
            "성탄 특별예배 안내",
            "신간 찬양앨범 출시",
            "정기후원 캠페인",
            "여름 수련회 모집"
    };
    private static final String[] LINK_URLS = {"/albums", "/donation", "/churches"};

    private static final BannerPosition[] POSITIONS = BannerPosition.values();
    private static final BannerDevice[] DEVICES = BannerDevice.values();

    private final BannerRepository bannerRepository;

    public BannerSeeder(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    public void run(String... args) {
        if (bannerRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);

        List<Banner> banners = new ArrayList<>();
        for (int i = 0; i < TARGET_BANNERS; i++) {
            BannerPosition position = POSITIONS[i % POSITIONS.length];
            BannerDevice device = DEVICES[i % DEVICES.length];

            LocalDateTime startAt = now.minusDays(rnd.nextInt(30));
            LocalDateTime endAt = startAt.plusDays(5 + rnd.nextInt(56)); // 5..60 days
            // ~20% already expired: pull the end date before now.
            if (rnd.nextInt(100) < 20) {
                endAt = now.minusDays(1 + rnd.nextInt(10));
            }
            String useYn = rnd.nextInt(100) < 80 ? "Y" : "N"; // ~80% visible

            banners.add(Banner.builder()
                    .title(TITLES[i % TITLES.length] + " " + (i + 1))
                    .position(position)
                    .device(device)
                    .imageUrl(imageUrl(position, i + 1))
                    .linkUrl(LINK_URLS[i % LINK_URLS.length])
                    .startAt(startAt)
                    .endAt(endAt)
                    .sortOrder(i)
                    .useYn(useYn)
                    .createdAt(startAt)
                    .build());
        }
        bannerRepository.saveAll(banners);
        log.info("Seeded {} banners", banners.size());
    }

    /** Placeholder image sized per placement slot (wide for main, tall for side, square for popup). */
    private String imageUrl(BannerPosition position, int n) {
        String size = switch (position) {
            case SIDE -> "300/600";
            case POPUP -> "600/600";
            default -> "1200/300";
        };
        return "https://picsum.photos/seed/banner" + n + "/" + size;
    }
}
