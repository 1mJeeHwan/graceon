package org.streamhub.api.base.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Provides a Jackson 2 {@link ObjectMapper} bean for the app's internal JSON work.
 *
 * <p>Spring Boot 4 makes Jackson 3 ({@code tools.jackson}) the default and no longer
 * auto-configures a {@code com.fasterxml.jackson.databind.ObjectMapper} bean. This app's adapters
 * (chat/payment/delivery providers, the action-log outbox relay, log archiver, and the JWT filter's
 * error responses) inject a Jackson 2 mapper, so we define one here. The HTTP layer still uses the
 * Boot-managed Jackson 3 mapper; both read the same {@code com.fasterxml.jackson.annotation}
 * annotations, so DTO serialization is unchanged.
 *
 * <p>Configured to match Boot 3.4's auto-configured defaults — ISO-8601 dates (not epoch
 * timestamps) and lenient deserialization — so the internal serialization behaviour is identical.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /**
     * Registers a Jackson 2 message converter on every auto-configured {@link
     * org.springframework.web.client.RestClient}. Boot 4's RestClient defaults to the Jackson 3
     * converter, which cannot read/write the Jackson 2 {@code com.fasterxml...JsonNode} (and DTOs)
     * that all the external-API adapters use ({@code .bodyTo(JsonNode.class)} on Kakao 장소검색/지오코딩,
     * Toss/Kakao/PayPal 결제, 배송조회, Gemini) — without this they throw on every call (e.g. the map
     * 교회검색 returned 500). Adding it at index 0 makes Jackson 2 types resolve first; Jackson 3 types
     * still fall through to the Jackson 3 converter.
     */
    @Bean
    public RestClientCustomizer jackson2RestClientCustomizer(ObjectMapper objectMapper) {
        return builder -> builder.messageConverters(converters ->
                converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper)));
    }
}
