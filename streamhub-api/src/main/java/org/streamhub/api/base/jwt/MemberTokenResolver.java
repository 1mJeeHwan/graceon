package org.streamhub.api.base.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Resolves the authenticated member id from a Bearer member token on public ({@code /pub/**})
 * endpoints. The public namespace is permitAll and the admin SecurityContext deliberately ignores
 * member tokens, so member-scoped public endpoints parse the token directly via this shared helper
 * instead of each controller re-implementing it. A missing/invalid member token is a 401.
 */
@Component
public class MemberTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public MemberTokenResolver(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Resolves the member id from the raw {@code Authorization} header value.
     *
     * @param authorization the {@code Authorization} header (may be null/blank)
     * @return the member id (JWT subject)
     * @throws ApiException {@code UNAUTHORIZED} if the header is missing/not Bearer,
     *                      {@code INVALID_TOKEN} if the token is not a member token
     */
    public Long resolve(String authorization) {
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
