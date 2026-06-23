package org.streamhub.api.v1.announcement;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.announcement.dto.AnnouncementDto;
import org.streamhub.api.v1.announcement.dto.AnnouncementSearchRequest;
import org.streamhub.api.v1.announcement.entity.Announcement;
import org.streamhub.api.v1.announcement.repository.AnnouncementRepository;

/**
 * Modal-ad announcements (안내창), managed like banners: list/detail/create/update/sort/delete for
 * the admin, and {@link #listPublicActive()} for the user site (enabled + within the display window,
 * in display order). Each row is one image popup ad.
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> list(AnnouncementSearchRequest request) {
        Boolean enabled = request == null ? null : request.enabled();
        return announcementRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(a -> enabled == null || a.isEnabled() == enabled)
                .map(AnnouncementDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementDto getDetail(Long id) {
        return AnnouncementDto.from(find(id));
    }

    /** Public read: only active ads (enabled + within the display window), in display order. */
    @Transactional(readOnly = true)
    public List<AnnouncementDto> listPublicActive() {
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(Announcement::isEnabled)
                .filter(a -> a.getStartAt() == null || !a.getStartAt().isAfter(now))
                .filter(a -> a.getEndAt() == null || !a.getEndAt().isBefore(now))
                .map(AnnouncementDto::from)
                .toList();
    }

    @Transactional
    public AnnouncementDto create(AnnouncementDto request) {
        Announcement saved = announcementRepository.save(Announcement.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .linkType(request.getLinkType())
                .linkRefId(request.getLinkRefId())
                .linkLabel(request.getLinkLabel())
                .linkUrl(request.getLinkUrl())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .sortOrder(request.getSortOrder())
                .enabled(request.isEnabled())
                .build());
        return AnnouncementDto.from(saved);
    }

    @Transactional
    public AnnouncementDto update(Long id, AnnouncementDto request) {
        Announcement announcement = find(id);
        announcement.update(
                request.getTitle(), request.getImageUrl(), request.getLinkType(),
                request.getLinkRefId(), request.getLinkLabel(), request.getLinkUrl(),
                request.getStartAt(), request.getEndAt(), request.getSortOrder(), request.isEnabled());
        return AnnouncementDto.from(announcementRepository.saveAndFlush(announcement));
    }

    @Transactional
    public AnnouncementDto updateSortOrder(Long id, int sortOrder) {
        Announcement announcement = find(id);
        announcement.updateSortOrder(sortOrder);
        return AnnouncementDto.from(announcementRepository.saveAndFlush(announcement));
    }

    @Transactional
    public void delete(Long id) {
        announcementRepository.delete(find(id));
    }

    private Announcement find(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
    }
}
