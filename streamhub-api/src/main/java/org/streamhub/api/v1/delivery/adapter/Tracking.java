package org.streamhub.api.v1.delivery.adapter;

import java.util.List;

/**
 * Live shipment status for an invoice (C8 delivery-tracking seam).
 *
 * @param carrierCode carrier code queried
 * @param carrierName carrier display name
 * @param invoiceNo   tracking (invoice) number
 * @param level       carrier progress level (1=접수 … 6=배달완료), best-effort
 * @param completed   whether delivery is complete
 * @param senderName  masked/raw sender name as reported (may be blank)
 * @param receiverName masked/raw receiver name as reported (may be blank)
 * @param events      chronological scan events (oldest first)
 */
public record Tracking(
        String carrierCode,
        String carrierName,
        String invoiceNo,
        int level,
        boolean completed,
        String senderName,
        String receiverName,
        List<TrackingEvent> events) {
}
