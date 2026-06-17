package org.streamhub.api.v1.worship.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.worship.entity.BaptismType;
import org.streamhub.api.v1.worship.entity.Gender;
import org.streamhub.api.v1.worship.entity.RegisterDept;
import org.streamhub.api.v1.worship.entity.RegistrationStatus;

/** Full registration detail. Base fields from MyBatis; family rows filled by the service. */
@Getter
@Setter
@NoArgsConstructor
public class WorshipRegistrationDetail {
    private Long id;
    private String regNo;
    private Long churchId;
    private String churchName;
    private RegistrationStatus status;
    private String name;
    private Gender gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String zipcode;
    private String addr1;
    private String addr2;
    private RegisterDept registerDept;
    private String churchExperience;
    private String prevChurch;
    private BaptismType baptismType;
    private String leaderName;
    private String leaderPhone;
    private String privacyAgreed;
    private String memo;
    private String testMode;
    private List<RegistrationFamilyDto> families;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
