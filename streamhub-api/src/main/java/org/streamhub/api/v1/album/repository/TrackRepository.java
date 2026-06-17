package org.streamhub.api.v1.album.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.album.entity.Track;

/** JPA repository for {@link Track} (album tracks). */
public interface TrackRepository extends JpaRepository<Track, Long> {

    List<Track> findByAlbumIdOrderByTrackNoAsc(Long albumId);

    Optional<Track> findByIdAndAlbumId(Long id, Long albumId);

    void deleteByAlbumId(Long albumId);
}
