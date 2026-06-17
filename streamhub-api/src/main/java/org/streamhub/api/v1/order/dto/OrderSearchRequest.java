package org.streamhub.api.v1.order.dto;

import java.time.LocalDate;
import org.streamhub.api.v1.order.entity.OrderStatus;

/**
 * Order list search + pagination request. All filters optional.
 *
 * <p>{@code searchField} selects which column {@code keyword} matches:
 * {@code orderNo} / {@code orderedName} / {@code receiverName} / {@code trackingNo}.
 */
public record OrderSearchRequest(
        Integer pageNumber,
        Integer pageSize,
        String searchField,
        String keyword,
        OrderStatus status,
        String payMethod,
        LocalDate fromDate,
        LocalDate toDate) {

    public int pageSizeOrDefault() {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    public int offset() {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        return page * pageSizeOrDefault();
    }
}
