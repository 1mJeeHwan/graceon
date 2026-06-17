package org.streamhub.api.v1.church.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.streamhub.api.v1.church.entity.Denomination;

/**
 * Create/update-church payload (shared). {@code worshipTimes} are dynamic rows replaced on
 * save (delete-then-reinsert). {@code thumbnailKey} comes from a prior /upload call. When
 * {@code latitude}/{@code longitude} are omitted, the service derives them via the geocode seam.
 */
public record ChurchUpsertRequest(
        @NotBlank(message = "교회명을 입력하세요") String name,
        @NotNull(message = "지역은 필수입니다") Long regionId,
        Denomination denomination,
        @DecimalMin(value = "-90.0", message = "위도 범위가 올바르지 않습니다")
        @DecimalMax(value = "90.0", message = "위도 범위가 올바르지 않습니다") Double latitude,
        @DecimalMin(value = "-180.0", message = "경도 범위가 올바르지 않습니다")
        @DecimalMax(value = "180.0", message = "경도 범위가 올바르지 않습니다") Double longitude,
        String address,
        String addressDetail,
        String zipcode,
        String phone,
        String pastorName,
        String facilities,
        String introduction,
        String homepageUrl,
        String thumbnailKey,
        String openYn,
        String useYn,
        List<WorshipTimeDto> worshipTimes) {
}
