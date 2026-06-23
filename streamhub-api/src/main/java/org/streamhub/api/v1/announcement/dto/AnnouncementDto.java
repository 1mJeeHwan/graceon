package org.streamhub.api.v1.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.announcement.entity.Announcement;
import org.streamhub.api.v1.banner.entity.BannerLinkType;

/**
 * A modal-ad announcement row. Used as both the admin create/update input and the list/detail/public
 * output (managed like a banner). Mutable to match the project DTO style.
 *
 * <p>Bean Validation mirrors the entity limits so bad input fails as {@code 400} (via {@code @Valid})
 * rather than a raw DB {@code 500}. The display window ({@code startAt}/{@code endAt}) is optional.
 */
@Getter
@Setter
@NoArgsConstructor
public class AnnouncementDto {

    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    /** Optional. Blank renders a text-only modal on the user site. */
    @Size(max = 500)
    private String imageUrl;

    /**
     * On a request: the raw URL for {@link BannerLinkType#URL} (or legacy). On a response: the
     * resolved click target — an internal path for VIDEO/MUSIC/POST, else the raw URL.
     */
    @Size(max = 500)
    private String linkUrl;

    /** Structured link target. Null = uses {@link #linkUrl} directly. */
    private BannerLinkType linkType;

    /** Referenced content/post id for VIDEO/MUSIC/POST. */
    private Long linkRefId;

    /** Selected content's title (display only). */
    @Size(max = 200)
    private String linkLabel;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @PositiveOrZero
    private int sortOrder;

    /** Visibility toggle (안내창 노출 여부). */
    private boolean enabled;

    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted announcement (resolves the public click target). */
    public static AnnouncementDto from(Announcement a) {
        AnnouncementDto dto = new AnnouncementDto();
        dto.id = a.getId();
        dto.title = a.getTitle();
        dto.imageUrl = a.getImageUrl();
        dto.linkType = a.getLinkType();
        dto.linkRefId = a.getLinkRefId();
        dto.linkLabel = a.getLinkLabel();
        dto.linkUrl = resolveLink(a);
        dto.startAt = a.getStartAt();
        dto.endAt = a.getEndAt();
        dto.sortOrder = a.getSortOrder();
        dto.enabled = a.isEnabled();
        dto.createdAt = a.getCreatedAt();
        return dto;
    }

    /**
     * Resolves the public click target: an internal path for content link types, else the raw
     * {@code linkUrl} (covers {@link BannerLinkType#URL} and legacy rows with a null type).
     */
    private static String resolveLink(Announcement a) {
        BannerLinkType type = a.getLinkType();
        Long ref = a.getLinkRefId();
        if (type != null && ref != null) {
            switch (type) {
                case VIDEO:
                    return "/video/" + ref;
                case MUSIC:
                    return "/music/" + ref;
                case POST:
                    return "/posts/" + ref;
                default:
                    break;
            }
        }
        return a.getLinkUrl();
    }
}
