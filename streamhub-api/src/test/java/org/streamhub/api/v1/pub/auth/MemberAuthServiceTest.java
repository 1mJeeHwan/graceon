package org.streamhub.api.v1.pub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.jwt.JwtTokenProvider;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.member.entity.Church;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.entity.UserStatus;
import org.streamhub.api.v1.member.repository.ChurchRepository;
import org.streamhub.api.v1.member.repository.MemberRepository;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.streamhub.api.v1.pub.auth.dto.MemberAuthResponse;
import org.streamhub.api.v1.pub.auth.dto.MemberLoginRequest;
import org.streamhub.api.v1.pub.auth.dto.MemberSignupRequest;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChurchRepository churchRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private org.streamhub.api.v1.security.SecurityMonitor securityMonitor;

    @Mock
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private org.streamhub.api.base.iamport.IamportCertService iamportCertService;

    @InjectMocks
    private MemberAuthService memberAuthService;

    private Member member(UserStatus status) {
        Member m = Member.builder()
                .churchId(1L)
                .email("member01@streamhub.test")
                .password("hash")
                .name("김민준")
                .phone("010-1000-2000")
                .userStatus(status)
                .liveYn("Y")
                .build();
        ReflectionTestUtils.setField(m, "id", 9L);
        return m;
    }

    @Test
    void login_confirmedMemberWithRightPassword_issuesTokenAndProfile() {
        Member m = member(UserStatus.CONFIRMED);
        when(memberRepository.findByEmail("member01@streamhub.test")).thenReturn(Optional.of(m));
        when(passwordEncoder.matches("member1234", "hash")).thenReturn(true);
        when(tokenProvider.createMemberAccessToken(m)).thenReturn("member.jwt");
        when(tokenProvider.getMemberExpSeconds()).thenReturn(28800L);
        when(churchRepository.findById(1L))
                .thenReturn(Optional.of(Church.builder().regionId(1L).name("서울중앙교회").openYn("Y").build()));

        MemberAuthResponse res = memberAuthService.login(
                new MemberLoginRequest("member01@streamhub.test", "member1234"));

        assertThat(res.token()).isEqualTo("member.jwt");
        assertThat(res.expiresIn()).isEqualTo(28800L);
        assertThat(res.member().name()).isEqualTo("김민준");
        assertThat(res.member().churchName()).isEqualTo("서울중앙교회");
    }

    @Test
    void login_wrongPassword_throwsLoginFailed() {
        Member m = member(UserStatus.CONFIRMED);
        when(memberRepository.findByEmail("member01@streamhub.test")).thenReturn(Optional.of(m));
        when(passwordEncoder.matches("nope", "hash")).thenReturn(false);

        assertThatThrownBy(() -> memberAuthService.login(
                new MemberLoginRequest("member01@streamhub.test", "nope")))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.LOGIN_FAILED);
    }

    @Test
    void login_unknownEmail_throwsLoginFailed() {
        when(memberRepository.findByEmail("ghost@streamhub.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberAuthService.login(
                new MemberLoginRequest("ghost@streamhub.test", "x")))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.LOGIN_FAILED);
    }

    @Test
    void login_pendingMember_isForbidden() {
        Member pending = member(UserStatus.PENDING);
        when(memberRepository.findByEmail("member01@streamhub.test")).thenReturn(Optional.of(pending));
        // password check passes, but status gate must still reject
        lenient().when(passwordEncoder.matches("member1234", "hash")).thenReturn(true);

        assertThatThrownBy(() -> memberAuthService.login(
                new MemberLoginRequest("member01@streamhub.test", "member1234")))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.FORBIDDEN);
    }

    @Test
    void signup_verifiedNewMember_createsConfirmedAndIssuesToken() {
        MemberSignupRequest req = new MemberSignupRequest(
                "New@Streamhub.test", "password1", "신규회원", "010-9999-8888",
                true, true, true);
        when(memberRepository.existsByEmail("new@streamhub.test")).thenReturn(false);
        when(memberRepository.existsByPhone("010-9999-8888")).thenReturn(false);
        when(churchRepository.findAll())
                .thenReturn(List.of(Church.builder().regionId(1L).name("서울중앙교회").openYn("Y").build()));
        when(passwordEncoder.encode("password1")).thenReturn("hash");
        when(tokenProvider.createMemberAccessToken(org.mockito.ArgumentMatchers.any())).thenReturn("member.jwt");
        when(tokenProvider.getMemberExpSeconds()).thenReturn(28800L);

        MemberAuthResponse res = memberAuthService.signup(req);

        assertThat(res.token()).isEqualTo("member.jwt");
        assertThat(res.member().email()).isEqualTo("new@streamhub.test");
        verify(phoneVerificationService).consumeVerified("010-9999-8888");
        verify(memberRepository).save(org.mockito.ArgumentMatchers.any(Member.class));
    }

    @Test
    void signup_duplicateEmail_throwsInvalidParameter() {
        MemberSignupRequest req = new MemberSignupRequest(
                "dupe@streamhub.test", "password1", "신규회원", "010-9999-8888",
                true, true, false);
        when(memberRepository.existsByEmail("dupe@streamhub.test")).thenReturn(true);

        assertThatThrownBy(() -> memberAuthService.signup(req))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_PARAMETER);
    }

    @Test
    void me_returnsProfileForExistingMember() {
        Member m = member(UserStatus.CONFIRMED);
        when(memberRepository.findById(9L)).thenReturn(Optional.of(m));
        when(churchRepository.findById(1L))
                .thenReturn(Optional.of(Church.builder().regionId(1L).name("서울중앙교회").openYn("Y").build()));

        assertThat(memberAuthService.me(9L).email()).isEqualTo("member01@streamhub.test");
    }
}
