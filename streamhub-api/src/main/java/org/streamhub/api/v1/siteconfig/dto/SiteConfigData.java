package org.streamhub.api.v1.siteconfig.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Typed shape of the dynamic UI settings stored as JSON in {@code SITE_CONFIG.data} and
 * served (read-only) to the user site at {@code GET /pub/v1/site-config}. Unknown fields
 * are ignored so older/newer payloads stay forward-compatible.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteConfigData {

    /** First-visit default theme ("dark" | "light"). The user's own toggle takes priority. */
    private String defaultTheme = "dark";

    /** Accent color as a CSS hex string (e.g. "#40C1DF"); injected as the --primary token. */
    private String accentColor = "#40C1DF";

    private Announcement announcement = new Announcement();

    /** Home sections in display order; disabled ones are hidden. */
    private List<HomeSection> homeSections = defaultSections();

    /** Album ids pinned to the home "추천 음반" row (empty = none). */
    private List<Long> featuredAlbumIds = List.of();

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Announcement {
        private boolean enabled = false;
        private String text = "";
        private String link = "";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HomeSection {
        /** Stable section key the user site renders against. */
        private String key;
        private boolean enabled = true;

        public HomeSection(String key, boolean enabled) {
            this.key = key;
            this.enabled = enabled;
        }
    }

    /** The four home sections in their default order, all visible. */
    public static List<HomeSection> defaultSections() {
        return List.of(
                new HomeSection("worshipLive", true),
                new HomeSection("latestVideos", true),
                new HomeSection("ccmAlbums", true),
                new HomeSection("nearbyChurch", true));
    }

    /** A fresh default configuration (used when no row is seeded yet). */
    public static SiteConfigData defaults() {
        return new SiteConfigData();
    }
}
