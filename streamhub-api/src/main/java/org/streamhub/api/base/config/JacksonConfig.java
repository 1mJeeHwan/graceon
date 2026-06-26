package org.streamhub.api.base.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
