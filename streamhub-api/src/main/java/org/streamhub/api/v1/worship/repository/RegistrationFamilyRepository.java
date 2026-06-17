package org.streamhub.api.v1.worship.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.worship.entity.RegistrationFamily;

/** JPA repository for {@link RegistrationFamily} (worship registration family rows). */
public interface RegistrationFamilyRepository extends JpaRepository<RegistrationFamily, Long> {

    List<RegistrationFamily> findByRegistrationIdOrderBySortAscIdAsc(Long registrationId);
}
