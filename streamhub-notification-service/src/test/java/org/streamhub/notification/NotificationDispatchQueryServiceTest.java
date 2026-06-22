package org.streamhub.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Read-side unit tests: blank filters collapse to null, defaults apply when page/size are missing,
 * and the JPA {@link Page} maps cleanly to the {@link NotificationDispatchPage} contract.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatchQueryServiceTest {

    @Mock private NotificationDispatchRepository repository;

    private NotificationDispatchQueryService service() {
        return new NotificationDispatchQueryService(repository);
    }

    private NotificationDispatch entity() {
        return NotificationDispatch.builder()
                .channel("SMS").scope("BROADCAST").targetMasked("전체 회원")
                .title("예배 안내").content("주일 예배").status("SUCCESS")
                .build();
    }

    @Test
    void list_mapsPage_andAppliesDefaults() {
        Page<NotificationDispatch> page = new PageImpl<>(List.of(entity()), PageRequest.of(0, 10), 1);
        when(repository.search(isNull(), isNull(), isNull(), eq(PageRequest.of(0, 10)))).thenReturn(page);

        NotificationDispatchPage result = service().list(null, null, "  ", null, null);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.totalPage()).isEqualTo(1);
        assertThat(result.contents()).hasSize(1);
        assertThat(result.contents().get(0).channel()).isEqualTo("SMS");
        assertThat(result.contents().get(0).title()).isEqualTo("예배 안내");
    }

    @Test
    void list_passesFiltersAndPaging_through() {
        when(repository.search(eq("PUSH"), eq("FAIL"), eq("kim"), eq(PageRequest.of(1, 50))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 50), 0));

        service().list(1, 50, "PUSH", "FAIL", "kim");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq("PUSH"), eq("FAIL"), eq("kim"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }
}
