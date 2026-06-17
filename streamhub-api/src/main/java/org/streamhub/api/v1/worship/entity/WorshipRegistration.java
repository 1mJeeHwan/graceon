package org.streamhub.api.v1.worship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A worship/new-family online registration (C2). Owns a 1:N set of
 * {@link RegistrationFamily} rows. Status transitions are enforced by the service.
 */
@Entity
@Table(name = "WORSHIP_REGISTRATION", indexes = {
        @Index(name = "idx_worship_reg_church", columnList = "church_id"),
        @Index(name = "idx_worship_reg_status", columnList = "status"),
        @Index(name = "idx_worship_reg_created", columnList = "created_at"),
        @Index(name = "idx_worship_reg_no", columnList = "reg_no")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorshipRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → CHURCH. */
    @Column(name = "church_id", nullable = false)
    private Long churchId;

    /** {@code WR-yyyyMMdd-NNNN}. */
    @Column(name = "reg_no", nullable = false, unique = true, length = 30)
    private String regNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private RegistrationStatus status;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 6)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "zipcode", length = 10)
    private String zipcode;

    @Column(name = "addr1", length = 200)
    private String addr1;

    @Column(name = "addr2", length = 200)
    private String addr2;

    @Enumerated(EnumType.STRING)
    @Column(name = "register_dept", nullable = false, length = 16)
    private RegisterDept registerDept;

    /** {@code Y}/{@code N} — whether the applicant has prior church experience. */
    @Column(name = "church_experience", nullable = false, length = 1)
    private String churchExperience;

    @Column(name = "prev_church", length = 100)
    private String prevChurch;

    @Enumerated(EnumType.STRING)
    @Column(name = "baptism_type", nullable = false, length = 16)
    private BaptismType baptismType;

    @Column(name = "leader_name", length = 50)
    private String leaderName;

    @Column(name = "leader_phone", length = 20)
    private String leaderPhone;

    /** {@code Y} required (privacy consent). */
    @Column(name = "privacy_agreed", nullable = false, length = 1)
    private String privacyAgreed;

    /** Admin memo (latest value, not accumulated). */
    @Column(name = "memo", length = 500)
    private String memo;

    /** Always {@code Y} (demo marker). */
    @Column(name = "test_mode", nullable = false, length = 1)
    private String testMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private WorshipRegistration(Long churchId, String regNo, RegistrationStatus status, String name,
                                Gender gender, LocalDate birthDate, String phone, String email,
                                String zipcode, String addr1, String addr2, RegisterDept registerDept,
                                String churchExperience, String prevChurch, BaptismType baptismType,
                                String leaderName, String leaderPhone, String privacyAgreed,
                                String memo, String testMode, LocalDateTime createdAt) {
        this.churchId = churchId;
        this.regNo = regNo;
        this.status = status != null ? status : RegistrationStatus.RECEIVED;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.zipcode = zipcode;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.registerDept = registerDept;
        this.churchExperience = churchExperience != null ? churchExperience : "N";
        this.prevChurch = prevChurch;
        this.baptismType = baptismType != null ? baptismType : BaptismType.NONE;
        this.leaderName = leaderName;
        this.leaderPhone = leaderPhone;
        this.privacyAgreed = privacyAgreed;
        this.memo = memo;
        this.testMode = testMode != null ? testMode : "Y";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Transitions the registration status (legality enforced by the service). */
    public void changeStatus(RegistrationStatus to) {
        this.status = to;
        this.updatedAt = LocalDateTime.now();
    }

    /** Updates the admin memo. */
    public void updateMemo(String memo) {
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }
}
