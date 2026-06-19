package org.streamhub.api.v1.album.hls;

import org.springframework.data.jpa.repository.JpaRepository;

/** JPA repository for {@link HlsKey} — the server-only AES-128 keys for encrypted HLS streams. */
public interface HlsKeyRepository extends JpaRepository<HlsKey, Long> {
}
