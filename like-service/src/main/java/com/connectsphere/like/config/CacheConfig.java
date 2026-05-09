package com.connectsphere.like.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configures Cache infrastructure for the service.
 */
@Configuration
public class CacheConfig {

    public static final String LIKE_COUNTS_CACHE = "likeCounts";
    public static final String REACTION_SUMMARIES_CACHE = "reactionSummaries";
    public static final String USER_REACTIONS_CACHE = "userReactions";
/**
 * Performs the cache manager operation.
 * @param redisConnectionFactory method input parameter
 * @return resulting value
 */

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .prefixCacheNameWith("like:");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        LIKE_COUNTS_CACHE, defaults.entryTtl(Duration.ofMinutes(2)),
                        REACTION_SUMMARIES_CACHE, defaults.entryTtl(Duration.ofMinutes(2)),
                        USER_REACTIONS_CACHE, defaults.entryTtl(Duration.ofMinutes(1))
                ))
                .build();
    }
}
