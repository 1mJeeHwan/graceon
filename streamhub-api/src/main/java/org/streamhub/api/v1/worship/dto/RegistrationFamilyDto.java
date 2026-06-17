package org.streamhub.api.v1.worship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.streamhub.api.v1.worship.entity.RegistrationFamily;

/**
 * One family-member row, shared by the public create request and the admin detail response.
 *
 * @param name      family member name
 * @param relation  relation (배우자/자녀/부모 …) — free text
 * @param birthDate optional birth date
 */
public record RegistrationFamilyDto(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 20) String relation,
        LocalDate birthDate) {

    /** Maps a persisted entity to its DTO (admin detail). */
    public static RegistrationFamilyDto from(RegistrationFamily family) {
        return new RegistrationFamilyDto(family.getName(), family.getRelation(), family.getBirthDate());
    }
}
