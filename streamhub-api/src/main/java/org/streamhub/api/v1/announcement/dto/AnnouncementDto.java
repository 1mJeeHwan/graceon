package org.streamhub.api.v1.announcement.dto;

import org.streamhub.api.v1.announcement.entity.Announcement;

/**
 * Announcement config — used as both the read payload and the admin save request.
 *
 * @param enabled whether the announcement modal shows on the user site
 * @param text    the announcement body (shown in the modal)
 * @param linkUrl optional "자세히 보기" destination
 */
public record AnnouncementDto(boolean enabled, String text, String linkUrl) {

    public static AnnouncementDto from(Announcement announcement) {
        return new AnnouncementDto(
                announcement.isEnabled(), announcement.getText(), announcement.getLinkUrl());
    }

    /** Disabled default when no row exists yet. */
    public static AnnouncementDto disabled() {
        return new AnnouncementDto(false, null, null);
    }
}
