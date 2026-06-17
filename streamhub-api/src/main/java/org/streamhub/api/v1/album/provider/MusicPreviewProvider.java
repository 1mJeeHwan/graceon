package org.streamhub.api.v1.album.provider;

/**
 * Resolves a track's preview audio URL. Swapping the implementation flips the source
 * between seeded demo samples and a real music provider — the adapter seam that lets a
 * real API key be injected later without touching entities, DTOs, controllers, or the
 * frontend (C3 spec §3.5).
 */
public interface MusicPreviewProvider {

    /**
     * Resolves the playable preview URL for a track.
     *
     * @param albumId   album id
     * @param trackNo   track number (from 1)
     * @param storedUrl the {@code previewUrl} persisted on the track (may be null/blank)
     * @return a playable preview URL
     */
    String resolvePreviewUrl(Long albumId, int trackNo, String storedUrl);

    /** For the UI badge: true when previews are demo samples rather than a real source. */
    boolean isDemo();
}
