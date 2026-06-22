package org.streamhub.api.v1.announcement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.announcement.dto.AnnouncementDto;
import org.streamhub.api.v1.announcement.entity.Announcement;
import org.streamhub.api.v1.announcement.repository.AnnouncementRepository;

/**
 * Site announcement config (안내창). A single editable row: {@link #save} upserts it, {@link #get}
 * reads it (admin), and {@link #getPublic} returns it for the user site (disabled default when no
 * row exists, so the modal simply doesn't show).
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public AnnouncementDto get() {
        return announcementRepository.findTopByOrderByIdAsc()
                .map(AnnouncementDto::from)
                .orElseGet(AnnouncementDto::disabled);
    }

    /** Public read: identical shape; callers (user site) only act when {@code enabled} is true. */
    @Transactional(readOnly = true)
    public AnnouncementDto getPublic() {
        return get();
    }

    @Transactional
    public AnnouncementDto save(AnnouncementDto request) {
        Announcement announcement = announcementRepository.findTopByOrderByIdAsc().orElse(null);
        if (announcement == null) {
            announcement = Announcement.builder()
                    .enabled(request.enabled())
                    .text(request.text())
                    .linkUrl(request.linkUrl())
                    .build();
        } else {
            announcement.update(request.enabled(), request.text(), request.linkUrl());
        }
        return AnnouncementDto.from(announcementRepository.save(announcement));
    }
}
