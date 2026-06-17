package org.streamhub.api.v1.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.store.entity.Store;

/** JPA repository for {@link Store} (offline retail stores). */
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByUseYn(String useYn);

    List<Store> findByRegionId(Long regionId);
}
