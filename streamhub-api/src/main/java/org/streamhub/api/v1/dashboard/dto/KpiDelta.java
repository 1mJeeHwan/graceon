package org.streamhub.api.v1.dashboard.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single KPI card: current value, the comparison (previous-period) value, the
 * percentage delta between them, and an optional sparkline series for the mini chart.
 */
@Getter
@Setter
@NoArgsConstructor
public class KpiDelta {

    /** Current value. */
    private long current;

    /** Previous-period value used for the ▲▼ comparison. */
    private long previous;

    /** {@code (current - previous) / previous * 100}, rounded to 1 decimal; 0 when previous is 0. */
    private double deltaPct;

    /** Up to 7 trailing points for the mini sparkline; empty when not applicable. */
    private List<Long> spark = new ArrayList<>();

    /**
     * Builds a KPI card, computing {@link #deltaPct} from the two values.
     *
     * @param current  current value
     * @param previous previous-period value
     * @param spark    sparkline points (may be {@code null}, treated as empty)
     * @return a populated {@link KpiDelta}
     */
    public static KpiDelta of(long current, long previous, List<Long> spark) {
        KpiDelta kpi = new KpiDelta();
        kpi.current = current;
        kpi.previous = previous;
        kpi.deltaPct = previous == 0 ? 0.0 : Math.round((current - previous) / (double) previous * 1000.0) / 10.0;
        kpi.spark = spark != null ? spark : new ArrayList<>();
        return kpi;
    }
}
