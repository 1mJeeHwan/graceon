package org.streamhub.api.base.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Guards the Boot 4 RestClient ↔ Jackson 2 fix: without the {@code RestClientCustomizer} the
 * Boot-default (Jackson 3) RestClient cannot deserialize a Jackson 2 {@code JsonNode}, which made
 * every external-API adapter (Kakao 장소검색/지오코딩, 결제, 배송, Gemini) throw — e.g. the map
 * 교회검색 returned 500. With the customizer a {@code JsonNode} body round-trips.
 */
class JacksonConfigTest {

    @Test
    void restClient_withCustomizer_deserializesJackson2JsonNode() {
        JacksonConfig config = new JacksonConfig();
        RestClient.Builder builder = RestClient.builder();
        config.jackson2RestClientCustomizer(config.objectMapper()).customize(builder);

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/v2/local/search/keyword.json"))
                .andRespond(withSuccess("{\"documents\":[{\"place_name\":\"역삼교회\"}]}",
                        MediaType.APPLICATION_JSON));

        JsonNode body = builder.build().get()
                .uri("/v2/local/search/keyword.json")
                .retrieve()
                .body(JsonNode.class);

        assertThat(body).isNotNull();
        assertThat(body.path("documents").path(0).path("place_name").asText()).isEqualTo("역삼교회");
        server.verify();
    }
}
