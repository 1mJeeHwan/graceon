package org.streamhub.api.v1.album.entity;

/**
 * Preview audio source (C3 music seam). {@code SEED} = SoundHelix demo samples;
 * {@code EXTERNAL} = real music provider. Stored via {@code @Enumerated(STRING)}.
 */
public enum MusicSource {
    SEED,
    EXTERNAL
}
