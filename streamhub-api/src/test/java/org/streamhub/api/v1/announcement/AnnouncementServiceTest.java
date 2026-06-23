package org.streamhub.api.v1.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.streamhub.api.v1.announcement.dto.AnnouncementDto;
import org.streamhub.api.v1.announcement.entity.Announcement;
import org.streamhub.api.v1.announcement.repository.AnnouncementRepository;
import org.streamhub.api.v1.banner.entity.BannerLinkType;

/**
 * Unit tests for the public-facing modal-ad gating: {@link AnnouncementService#listPublicActive()}
 * returns only enabled ads within their display window, in order, and resolves structured links
 * (VIDEO/MUSIC/POST) to internal paths.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @InjectMocks private AnnouncementService service;

    private static Announcement ad(long id, boolean enabled, int sort, LocalDateTime start,
                                   LocalDateTime end, BannerLinkType type, Long ref, String url) {
        Announcement a = Announcement.builder()
                .title("ad" + id).imageUrl("https://img/" + id)
                .linkType(type).linkRefId(ref).linkUrl(url)
                .startAt(start).endAt(end).sortOrder(sort).enabled(enabled).build();
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    @Test
    void listPublicActive_keepsOnlyEnabledWithinWindow_inOrder_andResolvesLink() {
        LocalDateTime now = LocalDateTime.now();
        Announcement disabled = ad(1, false, 0, null, null, BannerLinkType.URL, null, "/x");
        Announcement future = ad(2, true, 1, now.plusDays(1), null, BannerLinkType.URL, null, "/x");
        Announcement expired = ad(3, true, 2, null, now.minusDays(1), BannerLinkType.URL, null, "/x");
        Announcement videoAd = ad(4, true, 3, null, null, BannerLinkType.VIDEO, 99L, null);
        when(announcementRepository.findAllByOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(disabled, future, expired, videoAd));

        List<AnnouncementDto> active = service.listPublicActive();

        assertThat(active).extracting(AnnouncementDto::getId).containsExactly(4L);
        // VIDEO link type resolves to the internal video path.
        assertThat(active.get(0).getLinkUrl()).isEqualTo("/video/99");
    }
}
