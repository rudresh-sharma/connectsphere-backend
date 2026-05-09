package com.connectsphere.follow.config;

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

    public static final String FOLLOW_COUNTS_CACHE = "followCounts";
    public static final String FOLLOW_STATUS_CACHE = "followStatus";
    public static final String FOLLOWING_IDS_CACHE = "followingIds";
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
                .prefixCacheNameWith("follow:");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        FOLLOW_COUNTS_CACHE, defaults.entryTtl(Duration.ofMinutes(2)),
                        FOLLOW_STATUS_CACHE, defaults.entryTtl(Duration.ofMinutes(1)),
                        FOLLOWING_IDS_CACHE, defaults.entryTtl(Duration.ofMinutes(2))
                ))
                .build();
    }
}
