package org.streamhub.api.v1.worship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A family member row of a {@link WorshipRegistration} (1:N, max 5 enforced by service). */
@Entity
@Table(name = "REGISTRATION_FAMILY", indexes = {
        @Index(name = "idx_reg_family_reg", columnList = "registration_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistrationFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → WORSHIP_REGISTRATION. */
    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** Relation (배우자/자녀/부모 …) — free text. */
    @Column(name = "relation", nullable = false, length = 20)
    private String relation;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Display order 1..5. */
    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Builder
    private RegistrationFamily(Long registrationId, String name, String relation,
                              LocalDate birthDate, Integer sort) {
        this.registrationId = registrationId;
        this.name = name;
        this.relation = relation;
        this.birthDate = birthDate;
        this.sort = sort != null ? sort : 0;
    }
}
