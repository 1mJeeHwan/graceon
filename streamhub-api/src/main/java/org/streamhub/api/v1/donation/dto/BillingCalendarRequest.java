package org.streamhub.api.v1.donation.dto;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Month selector for the billing-schedule calendar. Falls back to the current month
 * when {@code year}/{@code month} are not supplied.
 */
public record BillingCalendarRequest(
        Integer year,
        Integer month) {

    private YearMonth resolved() {
        if (year == null || month == null) {
            return YearMonth.now();
        }
        return YearMonth.of(year, month);
    }

    /** Inclusive start of the selected month. */
    public LocalDateTime from() {
        return resolved().atDay(1).atStartOfDay();
    }

    /** Inclusive end of the selected month (last second). */
    public LocalDateTime to() {
        return resolved().atEndOfMonth().atTime(23, 59, 59);
    }
}
