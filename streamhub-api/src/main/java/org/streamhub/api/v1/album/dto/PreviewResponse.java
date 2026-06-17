package org.streamhub.api.v1.album.dto;

/**
 * A track preview descriptor for the user-site mini player. The 30-second window is
 * enforced client-side via {@code startSec}/{@code lengthSec}. {@code demo} reflects
 * {@code MusicPreviewProvider.isDemo()} — true while running on SoundHelix samples, and
 * automatically false once a real external provider is wired in (honest-mode badge).
 *
 * @param previewUrl playable preview audio URL
 * @param startSec   preview start offset (seconds)
 * @param lengthSec  preview length (seconds, default 30)
 * @param demo       whether this is a demo sample rather than a real provider source
 */
public record PreviewResponse(String previewUrl, int startSec, int lengthSec, boolean demo) {
}
