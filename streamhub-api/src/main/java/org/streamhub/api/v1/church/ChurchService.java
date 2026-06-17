package org.streamhub.api.v1.church;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.external.geocode.GeocodeProvider;
import org.streamhub.api.base.external.geocode.GeocodeResult;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.church.dto.ChurchDetail;
import org.streamhub.api.v1.church.dto.ChurchListItem;
import org.streamhub.api.v1.church.dto.ChurchNearbyItem;
import org.streamhub.api.v1.church.dto.ChurchNearbyRequest;
import org.streamhub.api.v1.church.dto.ChurchSearchRequest;
import org.streamhub.api.v1.church.dto.ChurchUpsertRequest;
import org.streamhub.api.v1.church.dto.CodeLabel;
import org.streamhub.api.v1.church.dto.WorshipTimeDto;
import org.streamhub.api.v1.church.entity.Denomination;
import org.streamhub.api.v1.church.entity.WorshipTime;
import org.streamhub.api.v1.church.geo.HaversineDistance;
import org.streamhub.api.v1.church.mapper.ChurchMapper;
import org.streamhub.api.v1.church.repository.WorshipTimeRepository;
import org.streamhub.api.v1.member.entity.Church;
import org.streamhub.api.v1.member.repository.ChurchRepository;

/**
 * Church management: admin search/CRUD (JPA + MyBatis), worship-time replacement, and
 * public location-based search. Distance is computed with {@link HaversineDistance} after a
 * MyBatis bounding-box pre-filter. Coordinates may be derived via the {@link GeocodeProvider}
 * seam when an admin supplies only an address.
 */
@Service
public class ChurchService {

    /** {@code dataSource} marker that flags a record as demo/seed data. */
    private static final String SEED_SOURCE = "SEED";
    /** Degrees of latitude per kilometre (~111 km/deg). */
    private static final double KM_PER_LAT_DEGREE = 111.0;

    private final ChurchMapper churchMapper;
    private final ChurchRepository churchRepository;
    private final WorshipTimeRepository worshipTimeRepository;
    private final StorageService storageService;
    private final GeocodeProvider geocodeProvider;
    private final ActionLogPublisher actionLogPublisher;

    public ChurchService(
            ChurchMapper churchMapper,
            ChurchRepository churchRepository,
            WorshipTimeRepository worshipTimeRepository,
            StorageService storageService,
            GeocodeProvider geocodeProvider,
            ActionLogPublisher actionLogPublisher) {
        this.churchMapper = churchMapper;
        this.churchRepository = churchRepository;
        this.worshipTimeRepository = worshipTimeRepository;
        this.storageService = storageService;
        this.geocodeProvider = geocodeProvider;
        this.actionLogPublisher = actionLogPublisher;
    }

    // --- admin list / detail ------------------------------------------------

    @Transactional(readOnly = true)
    public ResInfinityList<ChurchListItem> list(ChurchSearchRequest request) {
        String keyword = blankToNull(request.keyword());
        String denomination = request.denomination() == null ? null : request.denomination().name();
        String useYn = blankToNull(request.useYn());
        int size = request.pageSizeOrDefault();

        List<ChurchListItem> contents = churchMapper.selectList(
                keyword, request.regionId(), denomination, useYn, request.offset(), size);
        contents.forEach(item -> item.setThumbnailUrl(storageService.publicUrl(item.getThumbnailKey())));
        long total = churchMapper.countList(keyword, request.regionId(), denomination, useYn);
        return ResInfinityList.of(contents, total, size);
    }

    @Transactional(readOnly = true)
    public ChurchDetail getDetail(Long id) {
        ChurchDetail detail = churchMapper.selectDetail(id);
        if (detail == null) {
            throw new ApiException(ResultCode.NOT_FOUND);
        }
        detail.setThumbnailUrl(storageService.publicUrl(detail.getThumbnailKey()));
        detail.setDemoData(SEED_SOURCE.equals(detail.getDataSource()));
        detail.setWorshipTimes(loadWorshipTimes(id));
        return detail;
    }

    /** Public detail: 404 unless visible ({@code use_yn = "Y"}). Includes worship times. */
    @Transactional(readOnly = true)
    public ChurchDetail getPublicDetail(Long id) {
        ChurchDetail detail = getDetail(id);
        if (!"Y".equals(detail.getUseYn())) {
            throw new ApiException(ResultCode.NOT_FOUND);
        }
        return detail;
    }

    /** Enum → Korean-label list for the denomination select box. */
    @Transactional(readOnly = true)
    public List<CodeLabel> listDenominations() {
        List<CodeLabel> labels = new ArrayList<>();
        for (Denomination d : Denomination.values()) {
            labels.add(new CodeLabel(d.name(), denominationLabel(d)));
        }
        return labels;
    }

    // --- public location search ---------------------------------------------

    /**
     * Location-based search. With coordinates + radius: bounding-box pre-filter, then precise
     * Haversine distance, radius cut, distance sort, and in-memory paging. Without coordinates:
     * region/denomination/keyword filters with {@code createdAt desc} and DB paging.
     */
    @Transactional(readOnly = true)
    public ResInfinityList<ChurchNearbyItem> nearby(ChurchNearbyRequest request) {
        String keyword = blankToNull(request.keyword());
        String denomination = request.denomination() == null ? null : request.denomination().name();
        int size = request.pageSizeOrDefault();

        if (!request.hasLocation()) {
            List<ChurchNearbyItem> items = churchMapper.selectPublicList(
                    keyword, request.regionId(), denomination, request.offset(), size);
            items.forEach(this::fillItemUrl);
            long total = churchMapper.countPublicList(keyword, request.regionId(), denomination);
            return ResInfinityList.of(items, total, size);
        }

        double lat = request.lat();
        double lng = request.lng();
        double radiusKm = request.radiusKmOrDefault();
        double latDelta = radiusKm / KM_PER_LAT_DEGREE;
        double lngDelta = radiusKm / (KM_PER_LAT_DEGREE * Math.cos(Math.toRadians(lat)));

        List<ChurchNearbyItem> candidates = churchMapper.selectInBox(
                lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta,
                keyword, request.regionId(), denomination);

        List<ChurchNearbyItem> hits = new ArrayList<>();
        for (ChurchNearbyItem item : candidates) {
            if (item.getLatitude() == null || item.getLongitude() == null) {
                // Coordinate-less church (incomplete geocode): keep it in the result with a null
                // distance so it is never NPE'd nor silently dropped — it just sorts last.
                item.setDistanceKm(null);
                fillItemUrl(item);
                hits.add(item);
                continue;
            }
            double distance = HaversineDistance.km(lat, lng, item.getLatitude(), item.getLongitude());
            if (distance <= radiusKm) {
                item.setDistanceKm(round2(distance));
                fillItemUrl(item);
                hits.add(item);
            }
        }
        // Nearest first; null-distance (coordinate-less) churches are ordered last.
        hits.sort(Comparator.comparing(ChurchNearbyItem::getDistanceKm,
                Comparator.nullsLast(Comparator.naturalOrder())));

        long total = hits.size();
        List<ChurchNearbyItem> page = paginate(hits, request.offset(), size);
        return ResInfinityList.of(page, total, size);
    }

    // --- admin CRUD ---------------------------------------------------------

    @Transactional
    public ChurchDetail create(ChurchUpsertRequest request) {
        Double latitude = request.latitude();
        Double longitude = request.longitude();
        String dataSource = SEED_SOURCE;
        if ((latitude == null || longitude == null)) {
            GeocodeResult geocode = geocodeProvider.geocode(request.address());
            latitude = geocode.latitude();
            longitude = geocode.longitude();
            dataSource = geocode.source();
        }

        Church church = Church.builder()
                .regionId(request.regionId())
                .name(request.name())
                .openYn(defaultYn(request.openYn()))
                .denomination(request.denomination())
                .latitude(latitude)
                .longitude(longitude)
                .address(request.address())
                .addressDetail(request.addressDetail())
                .zipcode(request.zipcode())
                .phone(request.phone())
                .pastorName(request.pastorName())
                .facilities(request.facilities())
                .introduction(request.introduction())
                .homepageUrl(request.homepageUrl())
                .thumbnailKey(request.thumbnailKey())
                .dataSource(dataSource)
                .useYn(defaultYn(request.useYn()))
                .build();
        Church saved = churchRepository.save(church);
        replaceWorshipTimes(saved.getId(), request.worshipTimes());
        actionLogPublisher.publish("CHURCH_CREATE", "CHURCH", String.valueOf(saved.getId()), request.name());
        return getDetail(saved.getId());
    }

    @Transactional
    public ChurchDetail update(Long id, ChurchUpsertRequest request) {
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        Double latitude = request.latitude();
        Double longitude = request.longitude();
        if (latitude == null || longitude == null) {
            GeocodeResult geocode = geocodeProvider.geocode(request.address());
            latitude = geocode.latitude();
            longitude = geocode.longitude();
        }
        church.update(
                request.name(), request.regionId(), request.denomination(), latitude, longitude,
                request.address(), request.addressDetail(), request.zipcode(), request.phone(),
                request.pastorName(), request.facilities(), request.introduction(),
                request.homepageUrl(), request.thumbnailKey(),
                defaultYn(request.openYn()), defaultYn(request.useYn()));
        churchRepository.saveAndFlush(church);
        replaceWorshipTimes(id, request.worshipTimes());
        actionLogPublisher.publish("CHURCH_UPDATE", "CHURCH", String.valueOf(id), request.name());
        return getDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        worshipTimeRepository.deleteByChurchId(id);
        storageService.delete(church.getThumbnailKey());
        churchRepository.delete(church);
        actionLogPublisher.publish("CHURCH_DELETE", "CHURCH", String.valueOf(id), church.getName());
    }

    // --- helpers ------------------------------------------------------------

    /** Replaces a church's worship times (delete-then-reinsert), like the goods options strategy. */
    private void replaceWorshipTimes(Long churchId, List<WorshipTimeDto> rows) {
        worshipTimeRepository.deleteByChurchId(churchId);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        int order = 0;
        for (WorshipTimeDto row : rows) {
            if (row == null || row.kind() == null) {
                continue;
            }
            worshipTimeRepository.save(WorshipTime.builder()
                    .churchId(churchId)
                    .kind(row.kind())
                    .dayLabel(row.dayLabel())
                    .startTime(row.startTime())
                    .place(row.place())
                    .target(row.target())
                    .sort(row.sort() != null ? row.sort() : order)
                    .build());
            order++;
        }
    }

    private List<WorshipTimeDto> loadWorshipTimes(Long churchId) {
        return worshipTimeRepository.findByChurchIdOrderBySortAscIdAsc(churchId).stream()
                .map(WorshipTimeDto::from)
                .toList();
    }

    private void fillItemUrl(ChurchNearbyItem item) {
        item.setThumbnailUrl(storageService.publicUrl(item.getThumbnailKey()));
    }

    private List<ChurchNearbyItem> paginate(List<ChurchNearbyItem> all, int offset, int size) {
        if (offset >= all.size()) {
            return new ArrayList<>();
        }
        int to = Math.min(offset + size, all.size());
        return new ArrayList<>(all.subList(offset, to));
    }

    private String denominationLabel(Denomination d) {
        return switch (d) {
            case METHODIST -> "감리교";
            case PCK -> "장로교(통합)";
            case HAPDONG -> "장로교(합동)";
            case HOLINESS -> "성결교";
            case GOSPEL -> "순복음";
            case BAPTIST -> "침례교";
            case ETC -> "기타";
        };
    }

    private String defaultYn(String value) {
        return blankToNull(value) == null ? "Y" : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
