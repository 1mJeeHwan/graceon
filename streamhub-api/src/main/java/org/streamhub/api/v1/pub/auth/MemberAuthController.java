package org.streamhub.api.v1.pub.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.base.util.ClientIpResolver;
import org.streamhub.api.base.jwt.MemberTokenDenylist;
import org.streamhub.api.base.jwt.MemberTokenResolver;
import org.streamhub.api.v1.analytics.PublicIngestRateLimiter;
import org.streamhub.api.v1.pub.auth.dto.MemberAuthResponse;
import org.streamhub.api.v1.pub.auth.dto.MemberInfo;
import org.streamhub.api.v1.pub.auth.dto.MemberLoginRequest;
import org.streamhub.api.v1.pub.auth.dto.MemberSignupRequest;

/**
 * End-user authentication endpoints under the public ({@code /pub/**}, permitAll) namespace.
 * {@code /me} authenticates by parsing the member token directly — it never relies on the
 * admin SecurityContext, which deliberately ignores member tokens.
 */
@Tag(name = "Member Auth", description = "사용자 사이트 로그인 (회원)")
@RestController
@RequestMapping("/pub/v1/auth")
public class MemberAuthController {

    /**
     * Token cost per signup/login attempt charged to the shared {@link PublicIngestRateLimiter}
     * (capacity 60, refill 5/s). At cost 12 an IP gets a burst of ~5 attempts and a steady ~25/min
     * before throttling — comfortable for humans, hostile to credential-stuffing/signup floods.
     */
    private static final int AUTH_RATE_COST = 12;

    private final MemberAuthService memberAuthService;
    private final MemberTokenResolver memberTokenResolver;
    private final MemberTokenDenylist tokenDenylist;
    private final PublicIngestRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public MemberAuthController(MemberAuthService memberAuthService,
                               MemberTokenResolver memberTokenResolver,
                               MemberTokenDenylist tokenDenylist,
                               PublicIngestRateLimiter rateLimiter,
                               ClientIpResolver clientIpResolver) {
        this.memberAuthService = memberAuthService;
        this.memberTokenResolver = memberTokenResolver;
        this.tokenDenylist = tokenDenylist;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Operation(summary = "회원 로그인", description = "이메일/비밀번호로 로그인하고 회원 토큰을 발급한다. 과도한 요청은 차단.")
    @PostMapping("/login")
    public ResultDTO<MemberAuthResponse> login(@Valid @RequestBody MemberLoginRequest request,
                                               HttpServletRequest httpRequest) {
        enforceRateLimit("memberLogin", httpRequest);
        return ResultDTO.ok(memberAuthService.login(request));
    }

    @Operation(summary = "회원가입",
            description = "약관 동의 후 회원을 생성하고 회원 토큰을 발급한다(가입 즉시 로그인). 과도한 요청은 차단.")
    @PostMapping("/signup")
    public ResultDTO<MemberAuthResponse> signup(@Valid @RequestBody MemberSignupRequest request,
                                                HttpServletRequest httpRequest) {
        enforceRateLimit("memberSignup", httpRequest);
        return ResultDTO.ok(memberAuthService.signup(request));
    }

    /**
     * Per-client-IP throttle for the unauthenticated auth endpoints. On exceed, rejects with
     * {@link ResultCode#INVALID_PARAMETER} (no dedicated 429 code exists) and a "too many requests"
     * message — mirroring {@code ChatController}.
     */
    private void enforceRateLimit(String bucket, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryAcquire(bucket + ":" + clientIpResolver.resolve(httpRequest), AUTH_RATE_COST)) {
            throw new ApiException(ResultCode.TOO_MANY_REQUESTS,
                    ResultCode.TOO_MANY_REQUESTS.getMessage());
        }
    }

    @Operation(summary = "내 정보", description = "회원 토큰으로 로그인한 회원의 프로필을 반환한다.")
    @GetMapping("/me")
    public ResultDTO<MemberInfo> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResultDTO.ok(memberAuthService.me(resolveMemberId(authorization)));
    }

    @Operation(summary = "회원 로그아웃",
            description = "제시된 회원 토큰을 만료 시각까지 폐기 목록에 올린다. 이후 같은 토큰은 거부된다. "
                    + "토큰이 이미 무효해도 200을 반환한다(로그아웃은 멱등).")
    @PostMapping("/logout")
    public ResultDTO<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            tokenDenylist.revoke(memberTokenResolver.verify(authorization));
        } catch (ApiException ignored) {
            // Already expired, malformed, or absent — the caller is logged out either way. Telling
            // them otherwise would only reveal whether a token was still valid.
        }
        return ResultDTO.ok();
    }

    /** Delegates to the shared resolver so the denylist check cannot be forgotten here. */
    private Long resolveMemberId(String authorization) {
        return memberTokenResolver.resolve(authorization);
    }
}
