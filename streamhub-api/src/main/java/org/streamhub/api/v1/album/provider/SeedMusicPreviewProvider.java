package org.streamhub.api.v1.album.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Default (active) preview provider. Returns SoundHelix demo samples — the same set as
 * {@code DataInitializer.SAMPLE_AUDIOS} — and is always honest about being a demo
 * ({@link #isDemo()} == true). A stored preview URL (seeded) wins; otherwise a track is
 * mapped deterministically so a data reset reproduces the same audio.
 *
 * <p>Active unless {@code app.music.provider=external} is set ({@code matchIfMissing=true}).
 */
@Component
@ConditionalOnProperty(name = "app.music.provider", havingValue = "seed", matchIfMissing = true)
public class SeedMusicPreviewProvider implements MusicPreviewProvider {

    /** SoundHelix demo samples — kept in sync with {@code DataInitializer.SAMPLE_AUDIOS}. */
    private static final String[] SAMPLES = {
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
    };

    @Override
    public String resolvePreviewUrl(Long albumId, int trackNo, String storedUrl) {
        if (StringUtils.hasText(storedUrl)) {
            return storedUrl;
        }
        long id = albumId != null ? albumId : 0L;
        int idx = (int) (((id * 7 + trackNo) % SAMPLES.length + SAMPLES.length) % SAMPLES.length);
        return SAMPLES[idx];
    }

    @Override
    public boolean isDemo() {
        return true;
    }
}
