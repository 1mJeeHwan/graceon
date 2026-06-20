package org.streamhub.api.v1.church;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.external.discovery.ChurchDiscoveryProvider;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.base.security.AuthoritiesConstants;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.church.dto.ChurchNearbyItem;
import org.streamhub.api.v1.church.dto.ChurchNearbyRequest;
import org.streamhub.api.v1.church.dto.ChurchSearchRequest;
import org.streamhub.api.v1.church.mapper.ChurchMapper;

/**
 * Unit tests for {@link ChurchService}: {@code nearby} null-coordinate robustness and coordinate
 * validation, plus the CHURCH_MANAGER multitenancy scope (a manager may touch only its own church;
 * directory creation is SYSTEM-only).
 */
@ExtendWith(MockitoExtension.class)
class ChurchServiceTest {

    private static final AdminPrincipal SYSTEM =
            new AdminPrincipal(1L, AuthoritiesConstants.SYSTEM, null);
    /** CHURCH_MANAGER scoped to church #1. */
    private static final AdminPrincipal MANAGER_1 =
            new AdminPrincipal(2L, AuthoritiesConstants.CHURCH_MANAGER, 1L);

    @Mock
    private ChurchMapper churchMapper;
    @Mock
    private StorageService storageService;
    @Mock
    private ChurchDiscoveryProvider discoveryProvider;

    @InjectMocks
    private ChurchService churchService;

    private ChurchNearbyItem item(long id, Double lat, Double lng) {
        ChurchNearbyItem it = new ChurchNearbyItem();
        it.setId(id);
        it.setName("교회" + id);
        it.setLatitude(lat);
        it.setLongitude(lng);
        return it;
    }

    @Test
    void nearby_nullCoordinateChurch_keptWithNullDistanceAndSortedLast() {
        // Origin near 강남 (37.4979, 127.0276). near=very close, far=~1km, noCoord=missing lat/lng.
        ChurchNearbyItem near = item(1L, 37.4980, 127.0277);
        ChurchNearbyItem far = item(2L, 37.5060, 127.0290);
        ChurchNearbyItem noCoord = item(3L, null, null);

        when(churchMapper.selectInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(), any(), any())).thenReturn(List.of(far, noCoord, near));
        lenient().when(storageService.publicUrl(anyString())).thenReturn(null);
        when(discoveryProvider.search(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Collections.emptyList());

        ChurchNearbyRequest request = new ChurchNearbyRequest(
                37.4979, 127.0276, 5.0, null, null, null, 0, 10);

        ResInfinityList<ChurchNearbyItem> result = churchService.nearby(request);
        List<ChurchNearbyItem> rows = result.getContents();

        // All three survive; nearest first, coordinate-less church last with null distance.
        assertThat(rows).extracting(ChurchNearbyItem::getId).containsExactly(1L, 2L, 3L);
        assertThat(rows.get(0).getDistanceKm()).isNotNull();
        assertThat(rows.get(1).getDistanceKm()).isNotNull();
        assertThat(rows.get(2).getDistanceKm()).isNull();
        assertThat(rows.get(0).getDistanceKm()).isLessThanOrEqualTo(rows.get(1).getDistanceKm());
    }

    @Test
    void nearby_outOfRangeCoordinates_rejected() {
        ChurchNearbyRequest bad = new ChurchNearbyRequest(
                999.0, 127.0, 5.0, null, null, null, 0, 10);
        ApiException ex = assertThrows(ApiException.class, () -> churchService.nearby(bad));
        assertThat(ex.getResultCode()).isEqualTo(ResultCode.INVALID_PARAMETER);
    }

    // --- multitenancy scope -------------------------------------------------

    @Test
    void getDetail_managerOtherChurch_notFound() {
        // MANAGER_1 (church #1) must not read church #2 — rejected before any DB load.
        ApiException ex = assertThrows(ApiException.class,
                () -> churchService.getDetail(2L, MANAGER_1));
        assertThat(ex.getResultCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void update_managerOtherChurch_notFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> churchService.update(2L, null, MANAGER_1));
        assertThat(ex.getResultCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void delete_managerOtherChurch_notFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> churchService.delete(2L, MANAGER_1));
        assertThat(ex.getResultCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void create_manager_forbidden() {
        // Only SYSTEM may mint a new directory entry.
        ApiException ex = assertThrows(ApiException.class,
                () -> churchService.create(null, MANAGER_1));
        assertThat(ex.getResultCode()).isEqualTo(ResultCode.FORBIDDEN);
    }

    @Test
    void list_manager_isScopedToOwnChurch() {
        when(churchMapper.selectList(any(), any(), any(), any(), eq(1L), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(churchMapper.countList(any(), any(), any(), any(), eq(1L))).thenReturn(0L);

        churchService.list(new ChurchSearchRequest(0, 10, null, null, null, null), MANAGER_1);

        // The own-church id is threaded into both the page and the count query.
        verify(churchMapper).selectList(any(), any(), any(), any(), eq(1L), anyInt(), anyInt());
        verify(churchMapper).countList(any(), any(), any(), any(), eq(1L));
    }

    @Test
    void list_system_isUnscoped() {
        when(churchMapper.selectList(any(), any(), any(), any(), isNull(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(churchMapper.countList(any(), any(), any(), any(), isNull())).thenReturn(0L);

        churchService.list(new ChurchSearchRequest(0, 10, null, null, null, null), SYSTEM);

        // SYSTEM lists across all churches (no own-church filter).
        verify(churchMapper).selectList(any(), any(), any(), any(), isNull(), anyInt(), anyInt());
        verify(churchMapper).countList(any(), any(), any(), any(), isNull());
    }
}
