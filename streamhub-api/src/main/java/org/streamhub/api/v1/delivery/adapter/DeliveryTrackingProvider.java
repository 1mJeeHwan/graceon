package org.streamhub.api.v1.delivery.adapter;

import java.util.List;

/**
 * Delivery-tracking seam (C8). The default {@link SweetTrackerDeliveryProvider} calls the real
 * SweetTracker (스마트택배) aggregator API; {@link MockDeliveryTrackingProvider} returns a
 * deterministic fake timeline for offline/demo use. Selected via {@code app.delivery.provider}.
 */
public interface DeliveryTrackingProvider {

    /** Provider code ({@code SWEETTRACKER} / {@code MOCK}). */
    String code();

    /** The list of supported courier companies. */
    List<Carrier> carriers();

    /** Best-guess carrier code for an invoice number, or {@code null} if undetermined. */
    String recommendCarrier(String invoice);

    /** Live shipment status for a carrier + invoice. */
    Tracking track(String carrierCode, String invoice);
}
