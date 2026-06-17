package org.streamhub.api.v1.dashboard.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The stacked revenue/donation timeseries (default 90 days). Built for ApexCharts:
 * {@link #categories} are the x-axis labels and the three parallel lists are the
 * stacked series. Every day in the requested range is present (gaps filled with 0)
 * so the chart axis is never ragged.
 */
@Getter
@Setter
@NoArgsConstructor
public class TimeseriesResponse {

    /** x-axis labels, {@code yyyy-MM-dd}, one per day in the range. */
    private List<String> categories = new ArrayList<>();

    /** 굿즈 매출 series — daily settled order revenue (KRW). */
    private List<Long> goodsRevenue = new ArrayList<>();

    /** 정기후원 series — daily SUBSCRIPTION-type donations (KRW). */
    private List<Long> recurringDonation = new ArrayList<>();

    /** 단건후원 series — daily ONCE-type donations (KRW). */
    private List<Long> onceDonation = new ArrayList<>();
}
