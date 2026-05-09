package com.connectsphere.notification.config;

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

    public static final String RECIPIENT_NOTIFICATIONS_CACHE = "recipientNotifications";
    public static final String UNREAD_COUNTS_CACHE = "unreadCounts";
/**
 * Performs the cache manager operation.
 * @param redisConnectionFactory method input parameter
 * @return resulting value
 */

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .disableCachingNullValues()
                .prefixCacheNameWith("notification:");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        RECIPIENT_NOTIFICATIONS_CACHE, defaults.entryTtl(Duration.ofMinutes(1)),
                        UNREAD_COUNTS_CACHE, defaults.entryTtl(Duration.ofMinutes(1))
                ))
                .build();
    }
}
