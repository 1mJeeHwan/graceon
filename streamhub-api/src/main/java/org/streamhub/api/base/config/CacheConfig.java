package org.streamhub.api.base.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
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
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** Short-term nearby-church discovery (Kakao POI) cache name. */
    static final String CHURCH_DISCOVERY_CACHE = "churchDiscovery";

    /** Dashboard/statistics aggregates: short TTL, they move constantly. */
    private static final Duration STATS_TTL = Duration.ofSeconds(60);

    /**
     * Nearby-church POI results: minutes, not seconds. These come from an external, rate-limited,
     * billed API and describe places that do not move — a 60-second TTL spent the quota without
     * buying anything.
     */
    private static final Duration DISCOVERY_TTL = Duration.ofMinutes(30);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig =
                baseConfig(typedMapper(ObjectMapper.DefaultTyping.NON_FINAL), STATS_TTL);
        RedisCacheConfiguration discoveryConfig =
                baseConfig(typedMapper(ObjectMapper.DefaultTyping.EVERYTHING), DISCOVERY_TTL);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CHURCH_DISCOVERY_CACHE, discoveryConfig)
                .build();
    }

    /**
     * Degrades cache outages to "slow", not "down".
     *
     * <p>Spring's default handler rethrows, so an unreachable Redis turned every {@code @Cacheable}
     * read into a 500 — taking down the dashboard and the public church search even though both can
     * be computed from the database alone. Everything behind these caches is derived data with an
     * authoritative source, so a lookup failure should fall through to the method and a write
     * failure should be dropped. This also matches how the login lockout counter already treats
     * Redis (fail-open); the two were previously inconsistent for no stated reason.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache get failed ({}#{}), recomputing from source: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache put failed ({}#{}): {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                // Logged at WARN on purpose: a missed eviction leaves stale data until the TTL
                // expires, which is a correctness smell rather than a mere slowdown.
                log.warn("Cache evict failed ({}#{}), stale until TTL: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache clear failed ({}): {}", cache.getName(), e.getMessage());
            }
        };
    }

    private RedisCacheConfiguration baseConfig(ObjectMapper mapper, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
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
