package org.streamhub.api.v1.siteconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.siteconfig.dto.SiteConfigData;
import org.streamhub.api.v1.siteconfig.repository.SiteConfigRepository;

/**
 * Seeds the singleton site-config row with a sensible default (dark theme, cyan accent,
 * a sample announcement, all home sections visible) so the user site and the admin editor
 * have content on first boot. Idempotent — skips when the row already exists.
 */
@Slf4j
@Component
@Order(20)
public class SiteConfigSeeder implements CommandLineRunner {

    private final SiteConfigRepository repository;
    private final SiteConfigService siteConfigService;

    public SiteConfigSeeder(SiteConfigRepository repository, SiteConfigService siteConfigService) {
        this.repository = repository;
        this.siteConfigService = siteConfigService;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        SiteConfigData data = SiteConfigData.defaults();
        data.getAnnouncement().setEnabled(true);
        data.getAnnouncement().setText("성탄 특별예배 안내 — 자세히 보기");
        data.getAnnouncement().setLink("/churches");
        siteConfigService.save(data);
        log.info("Seeded default site config");
    }
}
