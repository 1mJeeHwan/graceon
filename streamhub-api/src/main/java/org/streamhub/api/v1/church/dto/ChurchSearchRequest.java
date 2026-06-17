package org.streamhub.api.v1.church.dto;

import org.streamhub.api.v1.church.entity.Denomination;

/**
 * Admin church-list search + pagination request. All filters optional.
 *
 * @param pageNumber   zero-based page index
 * @param pageSize     page size (defaults to 10)
 * @param keyword      name/address keyword
 * @param regionId     region filter
 * @param denomination denomination filter
 * @param useYn        visibility filter ({@code "Y"}/{@code "N"})
 */
public record ChurchSearchRequest(
        Integer pageNumber,
        Integer pageSize,
        String keyword,
        Long regionId,
        Denomination denomination,
        String useYn) {

    public int pageSizeOrDefault() {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    public int offset() {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        return page * pageSizeOrDefault();
    }
}
