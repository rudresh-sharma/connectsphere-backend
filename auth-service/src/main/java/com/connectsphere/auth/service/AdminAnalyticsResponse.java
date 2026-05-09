package com.connectsphere.auth.service;

import java.util.List;

/**
 * Provides Admin Analytics business operations.
 */
public record AdminAnalyticsResponse(
        long totalUsers,
        long activeUsers,
        long dailyActiveUsers,
        long totalPosts,
        List<TrendingHashtagSummary> trendingHashtags
) {
    public record TrendingHashtagSummary(
            Long hashtagId,
            String tag,
            long postCount
    ) {
    }
}
