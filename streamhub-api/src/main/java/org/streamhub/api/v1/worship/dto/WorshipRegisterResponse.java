package org.streamhub.api.v1.worship.dto;

/**
 * Minimal public-create result. Exposes only the new id and registration number so the
 * public form can show a confirmation without leaking admin fields.
 *
 * @param id    new registration id
 * @param regNo {@code WR-yyyyMMdd-NNNN}
 */
public record WorshipRegisterResponse(Long id, String regNo) {
}
