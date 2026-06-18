package org.streamhub.api.v1.delivery.adapter;

/**
 * A courier company (C8 delivery-tracking seam).
 *
 * @param code          carrier code used by the tracking API (e.g. {@code 04} = CJ대한통운)
 * @param name          display name (e.g. {@code CJ대한통운})
 * @param international whether the carrier handles international shipments
 */
public record Carrier(String code, String name, boolean international) {
}
