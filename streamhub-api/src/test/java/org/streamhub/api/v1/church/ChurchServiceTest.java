package org.streamhub.api.v1.church;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.streamhub.api.base.external.discovery.ChurchDiscoveryProvider;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.storage.StorageService;
import org.streamhub.api.v1.church.dto.ChurchNearbyItem;
import org.streamhub.api.v1.church.dto.ChurchNearbyRequest;
import org.streamhub.api.v1.church.mapper.ChurchMapper;

/**
 * Unit tests for {@link ChurchService#nearby} null-coordinate robustness: a candidate church with
 * missing lat/lng must not NPE the Haversine sort, and must surface with a {@code null} distance
 * ordered after every coordinate-bearing hit (never silently dropped).
 */
@ExtendWith(MockitoExtension.class)
class ChurchServiceTest {

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
}
