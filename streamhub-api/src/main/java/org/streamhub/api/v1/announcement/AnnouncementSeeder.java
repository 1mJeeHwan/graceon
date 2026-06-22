package org.streamhub.api.v1.announcement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.announcement.entity.Announcement;
import org.streamhub.api.v1.announcement.repository.AnnouncementRepository;

/**
 * Seeds the initial announcement config. Idempotent (skips when a row exists) and wrapped in
 * try/catch — a seeder must never crash production boot.
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
            if (announcementRepository.count() > 0) {
                return;
            }
            announcementRepository.save(Announcement.builder()
                    .enabled(true)
                    .text("성탄 특별예배 안내 — 자세히 보기")
                    .linkUrl("/churches")
                    .build());
            log.info("Seeded default announcement config");
        } catch (RuntimeException e) {
            log.warn("Announcement seeding skipped: {}", e.getMessage());
        }
    }
}
