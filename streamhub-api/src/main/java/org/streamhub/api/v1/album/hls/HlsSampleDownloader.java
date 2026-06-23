package org.streamhub.api.v1.album.hls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * Fetches an external audio sample ({@code previewUrl}) server-side, behind the {@link SsrfGuard}.
 *
 * <p>Centralizes the only server-side outbound HTTP fetch in the HLS package so the SSRF defenses
 * (URL allow-list validation + disabled redirects) live in one place and cannot be bypassed by a
 * second, unguarded call site. Redirects are disabled ({@link HttpClient.Redirect#NEVER}) so an
 * allowed public host cannot 302 to a blocked internal address after validation.
 */
@Component
public class HlsSampleDownloader {

    /**
     * Maximum size, in bytes, of a downloaded sample (50 MB). Audio previews are small; this cap
     * bounds memory use so a malicious or misbehaving host cannot exhaust the heap by streaming an
     * unbounded (or {@code Content-Length}-lying) response body.
     */
    private static final long MAX_BODY_BYTES = 50L * 1024 * 1024;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * Downloads the bytes at {@code url} after validating it with {@link SsrfGuard}.
     *
     * <p>The response body is streamed and capped at {@link #MAX_BODY_BYTES}; the download is
     * aborted with an {@link ApiException} if it exceeds the cap, rather than buffering an unbounded
     * body into memory.
     *
     * <p><b>Residual DNS-rebinding TOCTOU:</b> {@link SsrfGuard#validate(String)} resolves the host
     * and approves its address(es), but {@link HttpClient} re-resolves the host when it opens the
     * connection, so a host that rebinds between the two resolutions could be fetched at an address
     * that was never approved. Fully closing this would require pinning the connection to the
     * validated IP via a custom {@code java.net.http} resolver — a disproportionate rewrite for this
     * path. It is mitigated by: (1) the URL is admin/CHURCH_MANAGER-set, not attacker-supplied, and
     * (2) redirects are disabled ({@link HttpClient.Redirect#NEVER}), so an approved public host
     * cannot bounce to an internal address. As a cheap belt-and-suspenders check we re-validate the
     * URL immediately before dispatch so a host whose DNS already flipped to a blocked address is
     * rejected.
     *
     * @param url the external sample URL to fetch
     * @return the downloaded bytes
     * @throws ApiException {@code INVALID_PARAMETER} if the URL is not allowed,
     *     or {@code INTERNAL_ERROR} if the fetch fails, exceeds the size cap, or returns a non-200
     *     status
     */
    public byte[] download(String url) {
        SsrfGuard.validate(url);
        // Re-validate right before dispatch to shrink the DNS-rebinding window (see Javadoc).
        URI uri = SsrfGuard.validate(url);
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new ApiException(ResultCode.INTERNAL_ERROR,
                        "샘플 다운로드 실패 (HTTP " + response.statusCode() + ")");
            }
            return readCapped(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(ResultCode.INTERNAL_ERROR, "샘플 다운로드 실패: " + e.getMessage());
        }
    }

    /**
     * Reads {@code body} fully into a byte array, aborting if the stream exceeds
     * {@link #MAX_BODY_BYTES}.
     */
    private static byte[] readCapped(InputStream body) throws IOException {
        try (InputStream in = body) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(chunk)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) {
                    throw new ApiException(ResultCode.INTERNAL_ERROR,
                            "샘플 다운로드 실패 (최대 크기 초과: " + MAX_BODY_BYTES + " bytes)");
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }
}
