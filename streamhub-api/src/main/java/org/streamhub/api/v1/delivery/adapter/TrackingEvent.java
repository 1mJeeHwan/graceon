package org.streamhub.api.v1.delivery.adapter;

/**
 * One scan event in a shipment's progress (C8 delivery-tracking seam).
 *
 * @param time        event time string as reported by the carrier (e.g. {@code 2026-06-18 14:03})
 * @param location    where the scan happened (e.g. {@code 옥천HUB})
 * @param description status text (e.g. {@code 간선상차}, {@code 배달완료})
 */
public record TrackingEvent(String time, String location, String description) {
}
