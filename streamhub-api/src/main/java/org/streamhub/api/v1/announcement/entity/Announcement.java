package org.streamhub.api.v1.announcement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The site-wide announcement shown to users as a one-time modal (안내창). Modeled as a single
 * editable row: the admin toggles {@code enabled} and edits the text/link, and the user site reads
 * the current config instead of a hard-coded constant.
 */
@Entity
@Table(name = "ANNOUNCEMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "text", length = 500)
    private String text;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Announcement(boolean enabled, String text, String linkUrl) {
        this.enabled = enabled;
        this.text = text;
        this.linkUrl = linkUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(boolean enabled, String text, String linkUrl) {
        this.enabled = enabled;
        this.text = text;
        this.linkUrl = linkUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
