package org.streamhub.api.v1.donation.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.donation.entity.DonationStatus;
import org.streamhub.api.v1.donation.entity.DonationType;

/** One row of the donation list, joined with member and (for subscriptions) plan names. */
@Getter
@Setter
@NoArgsConstructor
public class DonationListItem {
    private Long id;
    private Long memberId;
    private String memberName;
    private Long subscriptionId;
    private String planName;
    private DonationType type;
    private Long amount;
    private Integer cycleNo;
    private DonationStatus status;
    private Long pointAwarded;
    private String testMode;
    private LocalDateTime paidAt;
}
