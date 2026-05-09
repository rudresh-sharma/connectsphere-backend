package com.connectsphere.comment.config;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configures Cache infrastructure for the service.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String POST_COMMENTS_CACHE = "postComments";
    public static final String COMMENT_DETAILS_CACHE = "commentDetails";
    public static final String COMMENT_REPLIES_CACHE = "commentReplies";
    public static final String COMMENT_COUNTS_CACHE = "commentCounts";
/**
 * Performs the cache manager operation.
 * @param redisConnectionFactory method input parameter
 * @return resulting value
 */

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(3))
                .disableCachingNullValues()
                .prefixCacheNameWith("comment:");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        POST_COMMENTS_CACHE, defaults.entryTtl(Duration.ofMinutes(1)),
                        COMMENT_DETAILS_CACHE, defaults.entryTtl(Duration.ofMinutes(3)),
                        COMMENT_REPLIES_CACHE, defaults.entryTtl(Duration.ofMinutes(1)),
                        COMMENT_COUNTS_CACHE, defaults.entryTtl(Duration.ofMinutes(2))
                ))
                .build();
    }
/**
 * Performs the error handler operation.
 * @return resulting value
 */

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
/**
 * Handles cache get error.
 * @param exception method input parameter
 * @param cache method input parameter
 * @param key method input parameter
 */
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Ignoring cache get failure for cache={} key={}: {}",
                        cache.getName(), key, exception.getMessage());
            }
/**
 * Handles cache put error.
 * @param exception method input parameter
 * @param cache method input parameter
 * @param key method input parameter
 * @param value method input parameter
 */

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Ignoring cache put failure for cache={} key={}: {}",
                        cache.getName(), key, exception.getMessage());
            }
/**
 * Handles cache evict error.
 * @param exception method input parameter
 * @param cache method input parameter
 * @param key method input parameter
 */

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Ignoring cache evict failure for cache={} key={}: {}",
                        cache.getName(), key, exception.getMessage());
            }
/**
 * Handles cache clear error.
 * @param exception method input parameter
 * @param cache method input parameter
 */

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Ignoring cache clear failure for cache={}: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
