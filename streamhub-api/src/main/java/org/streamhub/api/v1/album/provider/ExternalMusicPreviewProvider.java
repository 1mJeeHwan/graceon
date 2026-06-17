package org.streamhub.api.v1.album.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Real-provider injection point (inactive by default). Activated by
 * {@code app.music.provider=external}; the API key/base URL are injected from
 * {@code app.music.external.*}. The actual call to a real music provider (Melon/Bugs/
 * Spotify preview, etc.) is the only thing left to implement here — everything else in
 * the stack is provider-agnostic. Until then it fails loudly to keep demo mode honest.
 */
@Component
@ConditionalOnProperty(name = "app.music.provider", havingValue = "external")
public class ExternalMusicPreviewProvider implements MusicPreviewProvider {

    private final String apiKey;
    private final String baseUrl;

    public ExternalMusicPreviewProvider(
            @Value("${app.music.external.api-key:}") String apiKey,
            @Value("${app.music.external.base-url:}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public String resolvePreviewUrl(Long albumId, int trackNo, String storedUrl) {
        // Real integration goes here, calling baseUrl with apiKey. Not implemented in demo mode.
        boolean configured = !apiKey.isBlank() && !baseUrl.isBlank();
        String hint = configured ? "" : " (app.music.external.api-key/base-url 미설정)";
        throw new ApiException(ResultCode.INTERNAL_ERROR, "외부 음원 연동 미구현(데모 모드 사용)" + hint);
    }

    @Override
    public boolean isDemo() {
        return false;
    }
}
