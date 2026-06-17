package org.streamhub.api.v1.worship;

import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.worship.entity.RegistrationFamily;
import org.streamhub.api.v1.worship.entity.WorshipRegistration;
import org.streamhub.api.v1.worship.repository.RegistrationFamilyRepository;
import org.streamhub.api.v1.worship.repository.WorshipRegistrationRepository;

/**
 * Atomic persistence of a worship registration plus its family rows, isolated in its own
 * transaction so a {@code reg_no} unique-constraint collision can be retried cleanly.
 *
 * <p>The {@code reg_no} is allocated public + unauthenticated, so two same-day applicants can
 * race past the check-then-insert guard and violate the {@code uq reg_no} index. Each
 * {@link #insertWithRegNo} call runs in {@link Propagation#REQUIRES_NEW}: on collision the inner
 * transaction rolls back in isolation, leaving the caller's transaction (and persistence context)
 * intact so {@link WorshipService} can regenerate the number and re-attempt.
 */
@Component
public class WorshipRegistrationWriter {

    private final WorshipRegistrationRepository worshipRegistrationRepository;
    private final RegistrationFamilyRepository registrationFamilyRepository;

    public WorshipRegistrationWriter(
            WorshipRegistrationRepository worshipRegistrationRepository,
            RegistrationFamilyRepository registrationFamilyRepository) {
        this.worshipRegistrationRepository = worshipRegistrationRepository;
        this.registrationFamilyRepository = registrationFamilyRepository;
    }

    /**
     * Persists the registration carrying {@code regNo} together with its family rows in a fresh
     * transaction, flushing so a unique-constraint violation surfaces here (not on the outer commit).
     *
     * @param regNo          the candidate registration number to assign
     * @param registration   builds the registration entity from the assigned {@code regNo}
     * @param familyBuilder  builds the family rows once the saved registration id is known
     * @return the persisted registration (id populated)
     * @throws org.springframework.dao.DataIntegrityViolationException on a {@code reg_no} collision
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorshipRegistration insertWithRegNo(
            String regNo,
            Function<String, WorshipRegistration> registration,
            Function<Long, List<RegistrationFamily>> familyBuilder) {
        WorshipRegistration saved = worshipRegistrationRepository.saveAndFlush(registration.apply(regNo));
        List<RegistrationFamily> familyRows = familyBuilder.apply(saved.getId());
        if (!familyRows.isEmpty()) {
            registrationFamilyRepository.saveAll(familyRows);
        }
        return saved;
    }
}
