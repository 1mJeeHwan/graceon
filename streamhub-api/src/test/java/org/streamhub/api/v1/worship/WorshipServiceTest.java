package org.streamhub.api.v1.worship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.member.entity.Church;
import org.streamhub.api.v1.member.repository.ChurchRepository;
import org.streamhub.api.v1.worship.adapter.PostcodeProvider;
import org.streamhub.api.v1.worship.adapter.SmsNotifier;
import org.streamhub.api.v1.worship.dto.WorshipRegisterRequest;
import org.streamhub.api.v1.worship.dto.WorshipRegisterResponse;
import org.streamhub.api.v1.worship.entity.Gender;
import org.streamhub.api.v1.worship.entity.RegisterDept;
import org.streamhub.api.v1.worship.entity.WorshipRegistration;
import org.streamhub.api.v1.worship.mapper.WorshipMapper;
import org.streamhub.api.v1.worship.repository.RegistrationFamilyRepository;
import org.streamhub.api.v1.worship.repository.WorshipRegistrationRepository;

/**
 * Unit tests for {@link WorshipService} registration creation, focused on the {@code reg_no}
 * unique-collision retry (the public + unauthenticated path can race same-day applicants).
 */
@ExtendWith(MockitoExtension.class)
class WorshipServiceTest {

    @Mock
    private WorshipMapper worshipMapper;
    @Mock
    private WorshipRegistrationRepository worshipRegistrationRepository;
    @Mock
    private RegistrationFamilyRepository registrationFamilyRepository;
    @Mock
    private ChurchRepository churchRepository;
    @Mock
    private PostcodeProvider postcodeProvider;
    @Mock
    private SmsNotifier smsNotifier;
    @Mock
    private ActionLogPublisher actionLogPublisher;
    @Mock
    private WorshipRegistrationWriter worshipRegistrationWriter;

    @InjectMocks
    private WorshipService worshipService;

    private WorshipRegisterRequest request() {
        return new WorshipRegisterRequest(
                1L, "임지환", Gender.MALE, LocalDate.of(1990, 1, 1), "010-1234-5678",
                null, "06000", "서울특별시 강남구", "101호", RegisterDept.YOUTH,
                "N", null, null, null, null, "Y", List.of());
    }

    private void givenOpenChurch() {
        Church church = Church.builder().name("데모교회").openYn("Y").useYn("Y").build();
        when(churchRepository.findById(1L)).thenReturn(java.util.Optional.of(church));
        when(postcodeProvider.resolve(anyString(), anyString()))
                .thenReturn(new PostcodeProvider.PostcodeResult("06000", "서울특별시 강남구"));
    }

    private WorshipRegistration saved(String regNo) {
        WorshipRegistration reg = WorshipRegistration.builder()
                .churchId(1L).regNo(regNo).name("임지환").phone("010-1234-5678").privacyAgreed("Y").build();
        ReflectionTestUtils.setField(reg, "id", 42L);
        return reg;
    }

    @Test
    void create_retriesOnRegNoCollision_thenSucceeds() {
        givenOpenChurch();
        // First insert collides on the unique reg_no; the second attempt (fresh number) wins.
        when(worshipRegistrationWriter.insertWithRegNo(anyString(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uq reg_no"))
                .thenReturn(saved("WR-20260618-0002"));

        WorshipRegisterResponse response = worshipService.create(request());

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.regNo()).isEqualTo("WR-20260618-0002");
        verify(worshipRegistrationWriter, times(2)).insertWithRegNo(anyString(), any(), any());
        // Best-effort hooks still fire exactly once on the successful insert.
        verify(smsNotifier).notifyRegistrationReceived("010-1234-5678", "WR-20260618-0002");
    }

    @Test
    void create_persistentCollision_failsWithInternalError() {
        givenOpenChurch();
        when(worshipRegistrationWriter.insertWithRegNo(anyString(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uq reg_no"));

        assertThatThrownBy(() -> worshipService.create(request()))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INTERNAL_ERROR);

        verify(worshipRegistrationWriter, times(WorshipService.REG_NO_MAX_ATTEMPTS))
                .insertWithRegNo(anyString(), any(), any());
    }
}
