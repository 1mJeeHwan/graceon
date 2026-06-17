package org.streamhub.api.v1.album.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the {@code app.music.*} flags that select the active {@link MusicPreviewProvider}.
 * The provider itself is chosen via {@code @ConditionalOnProperty} on the implementations;
 * this type makes the configuration surface explicit and IDE-discoverable.
 *
 * <pre>
 * app:
 *   music:
 *     provider: ${MUSIC_PROVIDER:seed}   # seed (default) | external
 *     external:
 *       api-key: ${MUSIC_API_KEY:}
 *       base-url: ${MUSIC_BASE_URL:}
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "app.music")
public class MusicProviderConfig {

    /** Active provider id: {@code seed} (default) or {@code external}. */
    private String provider = "seed";

    /** External-provider connection settings (used only when {@code provider=external}). */
    private final External external = new External();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public External getExternal() {
        return external;
    }

    /** External music-provider settings. */
    public static class External {
        private String apiKey = "";
        private String baseUrl = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
