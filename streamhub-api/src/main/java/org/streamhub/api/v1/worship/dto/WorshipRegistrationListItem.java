package org.streamhub.api.v1.worship.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.worship.entity.Gender;
import org.streamhub.api.v1.worship.entity.RegisterDept;
import org.streamhub.api.v1.worship.entity.RegistrationStatus;

/** One row of the worship registration list, joined with the church name + family count. */
@Getter
@Setter
@NoArgsConstructor
public class WorshipRegistrationListItem {
    private Long id;
    private String regNo;
    private Long churchId;
    private String churchName;
    private RegistrationStatus status;
    private String name;
    private Gender gender;
    private RegisterDept registerDept;
    private String phone;
    private Integer familyCount;
    private String testMode;
    private LocalDateTime createdAt;
}
