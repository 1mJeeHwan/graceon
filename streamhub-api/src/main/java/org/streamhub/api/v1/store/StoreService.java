package org.streamhub.api.v1.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.store.dto.StoreDto;
import org.streamhub.api.v1.store.dto.StoreSearchRequest;
import org.streamhub.api.v1.store.entity.Store;
import org.streamhub.api.v1.store.repository.StoreRepository;

/**
 * Offline-store management: admin CRUD plus a public distance-sorted listing. The demo
 * dataset is small, so the public listing loads all visible stores and sorts in memory
 * by Haversine distance — no spatial index or external library needed (C3 spec §3.4).
 */
@Service
public class StoreService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StoreRepository storeRepository;
    private final ActionLogPublisher actionLogPublisher;

    public StoreService(StoreRepository storeRepository, ActionLogPublisher actionLogPublisher) {
        this.storeRepository = storeRepository;
        this.actionLogPublisher = actionLogPublisher;
    }

    /** Admin listing: all stores, newest first. */
    @Transactional(readOnly = true)
    public List<StoreDto> listAll() {
        return storeRepository.findAll().stream()
                .sorted(Comparator.comparing(Store::getId).reversed())
                .map(StoreDto::from)
                .toList();
    }

    /**
     * Public store-finder. Loads visible ({@code use_yn='Y'}) stores; sorts by distance
     * when coordinates are supplied (filling {@code distanceKm}), else filters by region.
     */
    @Transactional(readOnly = true)
    public List<StoreDto> listPublic(StoreSearchRequest request) {
        List<StoreDto> dtos = new ArrayList<>(
                storeRepository.findByUseYn("Y").stream().map(StoreDto::from).toList());
        if (request != null && request.lat() != null && request.lng() != null) {
            for (StoreDto dto : dtos) {
                dto.setDistanceKm(distanceKm(request.lat(), request.lng(), dto.getLat(), dto.getLng()));
            }
            dtos.sort(Comparator.comparing(
                    StoreDto::getDistanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if (request != null && request.regionId() != null) {
            dtos.removeIf(dto -> !request.regionId().equals(dto.getRegionId()));
        }
        return dtos;
    }

    @Transactional
    public StoreDto create(StoreDto request) {
        Store store = Store.builder()
                .regionId(request.getRegionId())
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .lat(request.getLat())
                .lng(request.getLng())
                .openHours(request.getOpenHours())
                .useYn(defaultYn(request.getUseYn()))
                .build();
        Store saved = storeRepository.save(store);
        actionLogPublisher.publish("STORE_CREATE", "STORE", String.valueOf(saved.getId()), request.getName());
        return StoreDto.from(saved);
    }

    @Transactional
    public StoreDto update(Long id, StoreDto request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        store.update(
                request.getRegionId(), request.getName(), request.getAddress(), request.getPhone(),
                request.getLat(), request.getLng(), request.getOpenHours(),
                defaultYn(request.getUseYn()));
        storeRepository.saveAndFlush(store);
        actionLogPublisher.publish("STORE_UPDATE", "STORE", String.valueOf(id), request.getName());
        return StoreDto.from(store);
    }

    @Transactional
    public void delete(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        storeRepository.delete(store);
        actionLogPublisher.publish("STORE_DELETE", "STORE", String.valueOf(id), store.getName());
    }

    // --- helpers -----------------------------------------------------------

    /** Great-circle distance (km) between two WGS84 points; null when a coordinate is missing. */
    private Double distanceKm(double fromLat, double fromLng,
                              java.math.BigDecimal toLat, java.math.BigDecimal toLng) {
        if (toLat == null || toLng == null) {
            return null;
        }
        double lat2 = toLat.doubleValue();
        double lng2 = toLng.doubleValue();
        double dLat = Math.toRadians(lat2 - fromLat);
        double dLng = Math.toRadians(lng2 - fromLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String defaultYn(String value) {
        return value == null || value.isBlank() ? "Y" : value;
    }
}
