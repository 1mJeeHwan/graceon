package org.streamhub.api.v1.worship.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.streamhub.api.v1.worship.entity.BaptismType;
import org.streamhub.api.v1.worship.entity.Gender;
import org.streamhub.api.v1.worship.entity.RegisterDept;

/**
 * Public (unauthenticated) worship/new-family registration submission. Validation here
 * covers structural constraints; business rules (church open, privacy consent, ≤5 family)
 * are enforced in the service.
 */
public record WorshipRegisterRequest(
        @NotNull Long churchId,
        @NotBlank @Size(max = 50) String name,
        @NotNull Gender gender,
        @NotNull LocalDate birthDate,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 120) String email,
        @Size(max = 10) String zipcode,
        @Size(max = 200) String addr1,
        @Size(max = 200) String addr2,
        @NotNull RegisterDept registerDept,
        @Size(max = 1) String churchExperience,
        @Size(max = 100) String prevChurch,
        BaptismType baptismType,
        @Size(max = 50) String leaderName,
        @Size(max = 20) String leaderPhone,
        @NotBlank @Size(max = 1) String privacyAgreed,
        @Valid List<RegistrationFamilyDto> families) {
}
