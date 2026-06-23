package org.streamhub.api.v1.announcement;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.announcement.entity.Announcement;
import org.streamhub.api.v1.announcement.repository.AnnouncementRepository;
import org.streamhub.api.v1.banner.entity.BannerLinkType;

/**
 * Seeds demo modal-ad announcements (안내창). Migrates the legacy single text-only row (no image) to
 * the new image-modal model, then seeds a couple of demo image ads if none exist. Idempotent and
 * wrapped in try/catch — a seeder must never crash production boot.
 */
@Slf4j
@Component
@Order(41)
public class AnnouncementSeeder implements CommandLineRunner {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementSeeder(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Override
    public void run(String... args) {
        try {
            // Migrate: drop the legacy text-only row(s) so the new image-modal seed can take over.
            announcementRepository.findByImageUrlIsNull()
                    .forEach(announcementRepository::delete);

            if (announcementRepository.existsByImageUrlIsNotNull()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            announcementRepository.save(Announcement.builder()
                    .title("성탄 특별예배 안내")
                    .imageUrl("https://picsum.photos/seed/notice1/720/900")
                    .linkType(BannerLinkType.URL)
                    .linkUrl("/churches")
                    .sortOrder(0)
                    .enabled(true)
                    .createdAt(now)
                    .build());
            announcementRepository.save(Announcement.builder()
                    .title("신규 찬양 앨범 발매")
                    .imageUrl("https://picsum.photos/seed/notice2/720/900")
                    .linkType(BannerLinkType.URL)
                    .linkUrl("/music")
                    .sortOrder(1)
                    .enabled(true)
                    .createdAt(now)
                    .build());
            log.info("Seeded demo announcement modal ads");
        } catch (RuntimeException e) {
            log.warn("Announcement seeding skipped: {}", e.getMessage());
        }
    }
}
