package org.streamhub.api.v1.church.dto;

/**
 * A code/label pair for enum-driven select boxes (e.g. denominations). The {@code code} is the
 * enum name the backend stores/searches; {@code label} is the Korean display text.
 *
 * @param code  enum name (e.g. {@code "METHODIST"})
 * @param label Korean label (e.g. {@code "감리교"})
 */
public record CodeLabel(String code, String label) {
}
