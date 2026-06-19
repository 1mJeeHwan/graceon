package org.streamhub.api.v1.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.streamhub.api.base.util.ClientIpResolver;
import org.streamhub.api.v1.security.entity.SecurityEvent;
import org.streamhub.api.v1.security.repository.SecurityEventRepository;

/**
 * Threshold behavior of {@link SecurityMonitor#recordAuthFailure}: below the limit only the
 * AUTH_FAILURE is stored; at the limit an additional HIGH SECURITY_ALERT is raised. Repository
 * and IP resolution are mocked.
 */
@ExtendWith(MockitoExtension.class)
class SecurityMonitorTest {

    private static final String IP = "203.0.113.7";
    private static final int THRESHOLD = 5;
    private static final int WINDOW_MINUTES = 10;

    @Mock
    private SecurityEventRepository securityEventRepository;

    @Mock
    private SecurityEventWriter securityEventWriter;

    @Mock
    private ClientIpResolver clientIpResolver;

    private SecurityMonitor securityMonitor;

    @BeforeEach
    void setUp() {
        securityMonitor = new SecurityMonitor(
                securityEventRepository, securityEventWriter, clientIpResolver, THRESHOLD, WINDOW_MINUTES);
        // Bind a request so currentIp() has a request thread to resolve from.
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn(IP);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordAuthFailure_belowThreshold_storesOnlyTheFailure() {
        // 4 prior failures already on record → still below the limit of 5.
        when(securityEventRepository.countByEventTypeAndIpAndCreatedAtAfter(
                eq("AUTH_FAILURE"), eq(IP), any(LocalDateTime.class))).thenReturn(4L);

        securityMonitor.recordAuthFailure("admin01", "ADMIN");

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventWriter, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("AUTH_FAILURE");
    }

    @Test
    void recordAuthFailure_atThreshold_alsoRaisesSecurityAlert() {
        // The just-recorded failure is the 5th from this IP → count returns 5 (>= threshold).
        when(securityEventRepository.countByEventTypeAndIpAndCreatedAtAfter(
                eq("AUTH_FAILURE"), eq(IP), any(LocalDateTime.class))).thenReturn(5L);

        securityMonitor.recordAuthFailure("admin01", "ADMIN");

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventWriter, times(2)).save(captor.capture());
        List<SecurityEvent> saved = captor.getAllValues();
        assertThat(saved.get(0).getEventType()).isEqualTo("AUTH_FAILURE");
        assertThat(saved.get(1).getEventType()).isEqualTo("SECURITY_ALERT");
        assertThat(saved.get(1).getSeverity()).isEqualTo("HIGH");
        assertThat(saved.get(1).getIp()).isEqualTo(IP);
    }

    @Test
    void recordAccessDenied_storesMediumEventWithPath() {
        securityMonitor.recordAccessDenied("/v1/members/9", "CHURCH_MANAGER", 3L);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventWriter, times(1)).save(captor.capture());
        SecurityEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("ACCESS_DENIED");
        assertThat(event.getSeverity()).isEqualTo("MEDIUM");
        assertThat(event.getPath()).isEqualTo("/v1/members/9");
        assertThat(event.getActorId()).isEqualTo(3L);
        // ACCESS_DENIED does not run the auth-failure threshold check.
        verify(securityEventRepository, never())
                .countByEventTypeAndIpAndCreatedAtAfter(anyString(), anyString(), any());
    }
}
