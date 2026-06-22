package org.streamhub.api.v1.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.media.entity.MediaAsset;
import org.streamhub.api.v1.media.repository.MediaAssetRepository;

/**
 * Seeds a handful of license-free (Unsplash) sample images so the media library isn't empty on a
 * fresh database. Idempotent (skips when rows exist) and wrapped in try/catch — a seeder must never
 * crash production boot. The stored key is an absolute URL, which {@code StorageService.publicUrl}
 * passes through unchanged.
 */
@Slf4j
@Component
@Order(40)
public class MediaAssetSeeder implements CommandLineRunner {

    private static final String Q = "?w=1200&q=80&auto=format&fit=crop";

    /** {url-id, category, originalName} — all ids verified to resolve (HTTP 200, image/jpeg). */
    private static final String[][] SAMPLES = {
            {"1492684223066-81342ee5ff30", "banner", "예배-공동체-배너.jpg"},
            {"1501281668745-f7f57925c3b4", "banner", "찬양-손들기-배너.jpg"},
            {"1526976668912-1a811878dd37", "banner", "소그룹-모임-배너.jpg"},
            {"1507692049790-de58290a4334", "banner", "찬양-무대-배너.jpg"},
            {"1438032005730-c779502df39b", "sample", "성전-스테인드글라스.jpg"},
            {"1473177104440-ffee2f376098", "sample", "대성당-내부.jpg"},
            {"1519491050282-cf00c82424b4", "sample", "예배당-의자.jpg"},
            {"1510590337019-5ef8d3d32116", "sample", "펼쳐진-성경.jpg"},
            {"1520523839897-bd0b52f945a0", "sample", "피아노-건반.jpg"},
            {"1465847899084-d164df4dedc6", "sample", "악보.jpg"},
            {"1544947950-fa07a98d237f", "sample", "도서-굿즈.jpg"},
            {"1556905055-8f358a7a47b2", "sample", "의류-굿즈.jpg"},
    };

    private final MediaAssetRepository mediaAssetRepository;

    public MediaAssetSeeder(MediaAssetRepository mediaAssetRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (mediaAssetRepository.count() > 0) {
                return;
            }
            for (String[] sample : SAMPLES) {
                mediaAssetRepository.save(MediaAsset.builder()
                        .storageKey("https://images.unsplash.com/photo-" + sample[0] + Q)
                        .category(sample[1])
                        .originalName(sample[2])
                        .contentType("image/jpeg")
                        .sizeBytes(0L)
                        .build());
            }
            log.info("Seeded {} media-library sample assets", SAMPLES.length);
        } catch (RuntimeException e) {
            log.warn("MediaAsset seeding skipped: {}", e.getMessage());
        }
    }
}
