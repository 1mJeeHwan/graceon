package org.streamhub.api.base.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Enables {@code @Cacheable} backed by Redis. Cached values are stored as JSON with
 * type metadata so DTOs round-trip cleanly. Default TTL is 60s — enough to make the
 * cache hit/miss behaviour observable for the statistics endpoints.
 *
 * <p>The default serializer types only non-final types ({@code NON_FINAL}); that is fine for the
 * DTO classes the stats/dashboard caches return. The {@code churchDiscovery} cache instead holds a
 * {@code List<DiscoveredChurch>} whose elements are <b>final records</b> — those need their own
 * serializer ({@code EVERYTHING}) so each element carries {@code @class} and deserializes back to a
 * record instead of a {@code LinkedHashMap}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Short-term nearby-church discovery (Kakao POI) cache name. */
    static final String CHURCH_DISCOVERY_CACHE = "churchDiscovery";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig =
                baseConfig(typedMapper(ObjectMapper.DefaultTyping.NON_FINAL));
        RedisCacheConfiguration discoveryConfig =
                baseConfig(typedMapper(ObjectMapper.DefaultTyping.EVERYTHING));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CHURCH_DISCOVERY_CACHE, discoveryConfig)
                .build();
    }

    private RedisCacheConfiguration baseConfig(ObjectMapper mapper) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper)));
    }

    private ObjectMapper typedMapper(ObjectMapper.DefaultTyping typing) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                typing);
        return mapper;
    }
}
