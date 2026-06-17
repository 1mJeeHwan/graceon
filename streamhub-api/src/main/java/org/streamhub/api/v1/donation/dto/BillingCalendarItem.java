package org.streamhub.api.v1.donation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One calendar day's aggregated billing forecast (active subscriptions due that day). */
@Getter
@Setter
@NoArgsConstructor
public class BillingCalendarItem {
    private String date; // yyyy-MM-dd
    private long count;
    private long amount;
}
