package org.streamhub.api.v1.siteconfig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Singleton site-configuration row (id is always {@link #SINGLETON_ID}). Stores the
 * dynamic UI settings the admin edits — default theme, accent color, announcement bar,
 * home-section order/visibility and featured albums — as a JSON blob in {@code data}.
 * A JSON column keeps new settings additive without schema migrations (demo-friendly);
 * the typed shape is validated at the service boundary via the config DTO.
 */
@Entity
@Table(name = "SITE_CONFIG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteConfig {

    /** The one and only row id — the site has a single configuration. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Lob
    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private SiteConfig(String data, LocalDateTime updatedAt) {
        this.id = SINGLETON_ID;
        this.data = data;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /** Replaces the serialized config blob and stamps the update time. */
    public void update(String data) {
        this.data = data;
        this.updatedAt = LocalDateTime.now();
    }
}
