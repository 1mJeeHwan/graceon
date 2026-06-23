package org.streamhub.api.v1.pub.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.jwt.JwtTokenProvider;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.pub.auth.dto.MemberAuthResponse;
import org.streamhub.api.v1.pub.auth.dto.MemberInfo;
import org.streamhub.api.v1.pub.auth.dto.MemberLoginRequest;
import org.streamhub.api.v1.pub.auth.dto.MemberSignupRequest;
import org.streamhub.api.v1.pub.auth.dto.PhoneConfirmRequest;
import org.streamhub.api.v1.pub.auth.dto.PhoneVerifyRequest;
import org.streamhub.api.v1.pub.auth.dto.PhoneVerifyResponse;

/**
 * End-user authentication endpoints under the public ({@code /pub/**}, permitAll) namespace.
 * {@code /me} authenticates by parsing the member token directly — it never relies on the
 * admin SecurityContext, which deliberately ignores member tokens.
 */
@Tag(name = "Member Auth", description = "사용자 사이트 로그인 (회원)")
@RestController
@RequestMapping("/pub/v1/auth")
public class MemberAuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final MemberAuthService memberAuthService;
    private final PhoneVerificationService phoneVerificationService;
    private final JwtTokenProvider tokenProvider;

    public MemberAuthController(MemberAuthService memberAuthService,
                               PhoneVerificationService phoneVerificationService,
                               JwtTokenProvider tokenProvider) {
        this.memberAuthService = memberAuthService;
        this.phoneVerificationService = phoneVerificationService;
        this.tokenProvider = tokenProvider;
    }

    @Operation(summary = "회원 로그인", description = "이메일/비밀번호로 로그인하고 회원 토큰을 발급한다.")
    @PostMapping("/login")
    public ResultDTO<MemberAuthResponse> login(@Valid @RequestBody MemberLoginRequest request) {
        return ResultDTO.ok(memberAuthService.login(request));
    }

    @Operation(summary = "휴대폰 본인인증 요청",
            description = "휴대폰 번호로 인증번호를 발급한다. 데모 환경에서는 응답에 인증번호를 포함한다.")
    @PostMapping("/verify/request")
    public ResultDTO<PhoneVerifyResponse> requestVerification(@Valid @RequestBody PhoneVerifyRequest request) {
        return ResultDTO.ok(phoneVerificationService.requestCode(request.phone()));
    }

    @Operation(summary = "휴대폰 본인인증 확인", description = "발급된 인증번호를 검증한다.")
    @PostMapping("/verify/confirm")
    public ResultDTO<Void> confirmVerification(@Valid @RequestBody PhoneConfirmRequest request) {
        phoneVerificationService.confirmCode(request.phone(), request.code());
        return ResultDTO.ok(null);
    }

    @Operation(summary = "회원가입",
            description = "본인인증을 마친 휴대폰으로 회원을 생성하고 회원 토큰을 발급한다(가입 즉시 로그인).")
    @PostMapping("/signup")
    public ResultDTO<MemberAuthResponse> signup(@Valid @RequestBody MemberSignupRequest request) {
        return ResultDTO.ok(memberAuthService.signup(request));
    }

    @Operation(summary = "내 정보", description = "회원 토큰으로 로그인한 회원의 프로필을 반환한다.")
    @GetMapping("/me")
    public ResultDTO<MemberInfo> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResultDTO.ok(memberAuthService.me(resolveMemberId(authorization)));
    }

    private Long resolveMemberId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }
        DecodedJWT jwt = tokenProvider.verify(authorization.substring(BEARER_PREFIX.length()));
        if (!tokenProvider.isMemberToken(jwt)) {
            throw new ApiException(ResultCode.INVALID_TOKEN);
        }
        return Long.valueOf(jwt.getSubject());
    }
}
