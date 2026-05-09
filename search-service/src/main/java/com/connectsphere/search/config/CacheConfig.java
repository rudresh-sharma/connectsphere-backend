package com.connectsphere.search.config;

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

    public static final String POST_SEARCHES_CACHE = "postSearches";
    public static final String POST_HASHTAGS_CACHE = "postHashtags";
    public static final String TRENDING_HASHTAGS_CACHE = "trendingHashtags";
    public static final String HASHTAG_POSTS_CACHE = "hashtagPosts";
    public static final String HASHTAG_SEARCHES_CACHE = "hashtagSearches";
    public static final String HASHTAG_COUNT_CACHE = "hashtagCount";
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
                .prefixCacheNameWith("search:");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        POST_SEARCHES_CACHE, defaults.entryTtl(Duration.ofMinutes(1)),
                        POST_HASHTAGS_CACHE, defaults.entryTtl(Duration.ofMinutes(5)),
                        TRENDING_HASHTAGS_CACHE, defaults.entryTtl(Duration.ofMinutes(2)),
                        HASHTAG_POSTS_CACHE, defaults.entryTtl(Duration.ofMinutes(2)),
                        HASHTAG_SEARCHES_CACHE, defaults.entryTtl(Duration.ofMinutes(2)),
                        HASHTAG_COUNT_CACHE, defaults.entryTtl(Duration.ofMinutes(5))
                ))
                .build();
    }
}
