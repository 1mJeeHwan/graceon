package org.streamhub.api.v1.album.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.album.entity.Album;

/** JPA repository for {@link Album} (CRUD). Listing/search uses MyBatis. */
public interface AlbumRepository extends JpaRepository<Album, Long> {
}
