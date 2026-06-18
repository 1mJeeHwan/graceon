package org.streamhub.api.v1.banner.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.banner.entity.Banner;

/** JPA repository for {@link Banner} (front promotional banners). */
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderBySortOrderAscIdAsc();
}
