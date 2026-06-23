package org.streamhub.api.v1.pub.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.jwt.JwtTokenProvider;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.member.entity.Church;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.entity.UserStatus;
import org.streamhub.api.v1.member.repository.ChurchRepository;
import org.streamhub.api.v1.member.repository.MemberRepository;
import org.streamhub.api.v1.pub.auth.dto.MemberAuthResponse;
import org.streamhub.api.v1.pub.auth.dto.MemberInfo;
import org.streamhub.api.v1.pub.auth.dto.MemberLoginRequest;
import org.streamhub.api.v1.pub.auth.dto.MemberSignupRequest;

/**
 * End-user (member) authentication for the public site. Issues member-scoped JWTs that
 * are isolated from admin tokens (see {@link JwtTokenProvider#createMemberAccessToken}).
 * Only {@link UserStatus#CONFIRMED} members may log in.
 */
@Service
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final org.streamhub.api.v1.security.SecurityMonitor securityMonitor;

    public MemberAuthService(
            MemberRepository memberRepository,
            ChurchRepository churchRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            org.streamhub.api.v1.security.SecurityMonitor securityMonitor) {
        this.memberRepository = memberRepository;
        this.churchRepository = churchRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.securityMonitor = securityMonitor;
    }

    /**
     * Registers a new member. Enforces email/phone uniqueness, stores a BCrypt password, and returns
     * an access token so the client is logged in immediately. New members are
     * {@link UserStatus#CONFIRMED} (self-service signup is auto-approved on this public site).
     */
    @Transactional
    public MemberAuthResponse signup(MemberSignupRequest request) {
        String phone = normalizePhone(request.phone());

        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByEmail(email)) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "이미 가입된 이메일입니다");
        }
        if (memberRepository.existsByPhone(phone)) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "이미 가입된 휴대폰 번호입니다");
        }

        Church church = churchRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ApiException(ResultCode.INTERNAL_ERROR, "가입 가능한 교회가 없습니다"));

        Member member = Member.builder()
                .churchId(church.getId())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .phone(phone)
                .userStatus(UserStatus.CONFIRMED)
                .liveYn("N")
                .marketingAgreed(request.agreeMarketing())
                .build();
        memberRepository.save(member);

        String token = tokenProvider.createMemberAccessToken(member);
        return new MemberAuthResponse(token, tokenProvider.getMemberExpSeconds(), toInfo(member));
    }

    /** Formats a phone as 010-XXXX-XXXX (matching seeded members) from any digit/hyphen input. */
    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return digits;
    }

    @Transactional(readOnly = true)
    public MemberAuthResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    securityMonitor.recordAuthFailure(request.email(), "MEMBER");
                    return new ApiException(ResultCode.LOGIN_FAILED);
                });
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            securityMonitor.recordAuthFailure(request.email(), "MEMBER");
            throw new ApiException(ResultCode.LOGIN_FAILED);
        }
        if (member.getUserStatus() != UserStatus.CONFIRMED) {
            throw new ApiException(ResultCode.FORBIDDEN, "승인 대기 중이거나 비활성화된 계정입니다");
        }
        String token = tokenProvider.createMemberAccessToken(member);
        return new MemberAuthResponse(token, tokenProvider.getMemberExpSeconds(), toInfo(member));
    }

    @Transactional(readOnly = true)
    public MemberInfo me(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return toInfo(member);
    }

    private MemberInfo toInfo(Member member) {
        String churchName = churchRepository.findById(member.getChurchId())
                .map(Church::getName)
                .orElse(null);
        return new MemberInfo(
                member.getId(), member.getName(), member.getEmail(),
                member.getPhone(), churchName, member.getCreatedAt());
    }
}
