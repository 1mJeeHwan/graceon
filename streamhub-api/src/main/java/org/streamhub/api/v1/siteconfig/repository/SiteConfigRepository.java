package org.streamhub.api.v1.siteconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.siteconfig.entity.SiteConfig;

/** JPA repository for the singleton {@link SiteConfig} row. */
public interface SiteConfigRepository extends JpaRepository<SiteConfig, Long> {
}
