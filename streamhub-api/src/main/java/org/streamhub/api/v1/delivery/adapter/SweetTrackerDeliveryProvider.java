package org.streamhub.api.v1.delivery.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;

/**
 * SweetTracker (스마트택배) delivery-tracking adapter (C8) — <b>real aggregator integration</b>
 * ({@code info.sweettracker.co.kr}). Default provider. The {@code t_key} is injected via
 * {@code DELIVERY_SWEETTRACKER_API_KEY} (not committed — public repo); get a free key or the public
 * docs demo key from SweetTracker. One aggregator covers every Korean courier, so no per-carrier
 * code is needed.
 */
@Component
@ConditionalOnProperty(name = "app.delivery.provider", havingValue = "sweettracker", matchIfMissing = true)
public class SweetTrackerDeliveryProvider implements DeliveryTrackingProvider {

    private static final String BASE = "http://info.sweettracker.co.kr/api/v1";

    private final String apiKey;
    private final RestClient restClient;

    public SweetTrackerDeliveryProvider(
            @Value("${app.delivery.sweettracker.api-key:}") String apiKey,
            RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String code() {
        return "SWEETTRACKER";
    }

    @Override
    public List<Carrier> carriers() {
        JsonNode node = get(UriComponentsBuilder.fromHttpUrl(BASE + "/companylist")
                .queryParam("t_key", apiKey).toUriString());
        List<Carrier> carriers = new ArrayList<>();
        JsonNode companies = node.get("Company");
        if (companies != null && companies.isArray()) {
            for (JsonNode c : companies) {
                carriers.add(new Carrier(
                        text(c, "Code"), text(c, "Name"),
                        "true".equalsIgnoreCase(text(c, "International"))));
            }
        }
        return carriers;
    }

    @Override
    public String recommendCarrier(String invoice) {
        JsonNode node = get(UriComponentsBuilder.fromHttpUrl(BASE + "/recommend")
                .queryParam("t_key", apiKey).queryParam("t_invoice", invoice).toUriString());
        JsonNode recommend = node.get("Recommend");
        if (recommend != null && recommend.isArray() && !recommend.isEmpty()) {
            return text(recommend.get(0), "Code");
        }
        return null;
    }

    @Override
    public Tracking track(String carrierCode, String invoice) {
        JsonNode node = get(UriComponentsBuilder.fromHttpUrl(BASE + "/trackingInfo")
                .queryParam("t_key", apiKey)
                .queryParam("t_code", carrierCode)
                .queryParam("t_invoice", invoice)
                .toUriString());

        // SweetTracker returns {status:false, msg:...} on bad input/quota.
        if (node.has("status") && node.get("status").isBoolean() && !node.get("status").asBoolean()) {
            String msg = node.hasNonNull("msg") ? node.get("msg").asText() : "배송조회 실패";
            throw new ApiException(ResultCode.INVALID_PARAMETER, "스마트택배: " + msg);
        }

        List<TrackingEvent> events = new ArrayList<>();
        JsonNode details = node.get("trackingDetails");
        if (details != null && details.isArray()) {
            for (JsonNode d : details) {
                events.add(new TrackingEvent(
                        firstText(d, "timeString", "time"),
                        firstText(d, "where", "location"),
                        firstText(d, "kind", "description")));
            }
        }
        boolean completed = "Y".equalsIgnoreCase(text(node, "completeYN"))
                || (node.hasNonNull("complete") && node.get("complete").asBoolean(false));
        int level = node.hasNonNull("level") ? node.get("level").asInt(1) : 1;
        return new Tracking(
                carrierCode, null, text(node, "invoiceNo"), level, completed,
                text(node, "senderName"), text(node, "receiverName"), events);
    }

    // --- helpers -----------------------------------------------------------

    private JsonNode get(String url) {
        ResponseEntity<JsonNode> response = restClient.get()
                .uri(url)
                .exchange((req, res) -> ResponseEntity
                        .status(res.getStatusCode())
                        .body(res.bodyTo(JsonNode.class)));
        JsonNode node = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || node == null) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "스마트택배 API 호출 실패");
        }
        return node;
    }

    private String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            String v = text(node, f);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
