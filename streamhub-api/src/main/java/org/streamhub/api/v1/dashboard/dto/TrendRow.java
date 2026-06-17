package org.streamhub.api.v1.dashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal row of the daily timeseries aggregation: one date with the revenue and
 * donation totals for that day. Sparse — only days with activity are returned; the
 * service fills the gaps with zeros across the full requested range.
 */
@Getter
@Setter
@NoArgsConstructor
public class TrendRow {

    /** Aggregation day, formatted {@code yyyy-MM-dd}. */
    private String date;

    /** Sum of goods order totals settled that day (KRW). */
    private long goodsRevenue;

    /** Sum of recurring (SUBSCRIPTION-type) donations paid that day (KRW). */
    private long recurringDonation;

    /** Sum of one-off (ONCE-type) donations paid that day (KRW). */
    private long onceDonation;
}
