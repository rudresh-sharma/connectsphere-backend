package com.connectsphere.follow.service.impl;

import com.connectsphere.follow.client.AuthServiceClient;
import com.connectsphere.follow.client.NotificationClient;
import com.connectsphere.follow.config.CacheConfig;
import com.connectsphere.follow.dto.FollowCountsResponse;
import com.connectsphere.follow.dto.FollowResponse;
import com.connectsphere.follow.dto.FollowStatusResponse;
import com.connectsphere.follow.dto.SuggestedUserResponse;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.exception.BadRequestException;
import com.connectsphere.follow.exception.ResourceNotFoundException;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.FollowService;
import com.connectsphere.follow.util.FollowMapper;

import lombok.Data;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Implements Follow business operations.
 */


@Service
@Transactional
@Data

public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final FollowRepository followRepository;
    private final CacheManager cacheManager;
    private final NotificationClient notificationClient;
    private final AuthServiceClient authServiceClient;

    public FollowServiceImpl(
            FollowRepository followRepository,
            CacheManager cacheManager,
            NotificationClient notificationClient,
            AuthServiceClient authServiceClient
    ) {
        this.followRepository = followRepository;
        this.cacheManager = cacheManager;
        this.notificationClient = notificationClient;
        this.authServiceClient = authServiceClient;
    }
/**
 * Performs the follow operation.
 * @param followerId entity identifier
 * @param followingId entity identifier
 * @return resulting value
 */

    @Override
    public FollowResponse follow(Long followerId, Long followingId) {
        validateUsers(followerId, followingId);

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BadRequestException("User is already followed");
        }

        String followerUsername = getUsername(followerId);

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        Follow saved = followRepository.save(follow);
        evictFollowCaches(followerId, followingId);

        notificationClient.sendFollowNotification(followingId, followerId, followerUsername);

        return FollowMapper.toResponse(saved);
    }

    private String getUsername(Long userId) {
        try {
            var user = authServiceClient.getUserById(userId);
            return user != null ? user.username() : "Someone";
        } catch (Exception ex) {
            log.warn("Could not fetch username for userId={}: {}", userId, ex.getMessage());
            return "Someone";
        }
    }
/**
 * Performs the unfollow operation.
 * @param followerId entity identifier
 * @param followingId entity identifier
 */

    @Override
    public void unfollow(Long followerId, Long followingId) {
        validateUsers(followerId, followingId);

        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found"));
        followRepository.delete(follow);
        evictFollowCaches(followerId, followingId);
    }
/**
 * Returns following.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<FollowResponse> getFollowing(Long userId, Pageable pageable) {
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(userId, pageable)
                .map(FollowMapper::toResponse);
    }
/**
 * Returns followers.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<FollowResponse> getFollowers(Long userId, Pageable pageable) {
        return followRepository.findByFollowingIdOrderByCreatedAtDesc(userId, pageable)
                .map(FollowMapper::toResponse);
    }
/**
 * Returns status.
 * @param followerId entity identifier
 * @param followingId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getStatus(Long followerId, Long followingId) {
        validateUsers(followerId, followingId);
        return new FollowStatusResponse(
                followerId,
                followingId,
                followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        );
    }
/**
 * Returns counts.
 * @param userId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public FollowCountsResponse getCounts(Long userId) {
        return new FollowCountsResponse(
                userId,
                followRepository.countByFollowingId(userId),
                followRepository.countByFollowerId(userId)
        );
    }
/**
 * Returns following ids.
 * @param userId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .stream()
                .map(Follow::getFollowingId)
                .toList();
    }
/**
 * Returns mutual following ids.
 * @param userId entity identifier
 * @param otherUserId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMutualFollowingIds(Long userId, Long otherUserId) {
        validateUsers(userId, otherUserId);
        return followRepository.findMutualFollowingIds(userId, otherUserId);
    }
/**
 * Returns suggested users.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestedUserResponse> getSuggestedUsers(Long userId, Pageable pageable) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId is required");
        }

        Set<Long> excludedUserIds = new LinkedHashSet<>(getFollowingIds(userId));
        excludedUserIds.add(userId);

        return followRepository.findSuggestedUserIds(userId, excludedUserIds, pageable)
                .map(row -> new SuggestedUserResponse((Long) row[0], ((Number) row[1]).longValue()));
    }
/**
 * Deletes user relationships.
 * @param userId entity identifier
 */

    @Override
    public void deleteUserRelationships(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        followRepository.deleteByFollowerIdOrFollowingId(userId, userId);
        clearFollowCaches();
    }

    private void validateUsers(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new BadRequestException("followerId and followingId are required");
        }
        if (followerId.equals(followingId)) {
            throw new BadRequestException("Users cannot follow themselves");
        }
    }

    private void evictFollowCaches(Long followerId, Long followingId) {
        evict(CacheConfig.FOLLOW_STATUS_CACHE, statusCacheKey(followerId, followingId));
        evict(CacheConfig.FOLLOW_COUNTS_CACHE, followerId);
        evict(CacheConfig.FOLLOW_COUNTS_CACHE, followingId);
        evict(CacheConfig.FOLLOWING_IDS_CACHE, followerId);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    private void clearFollowCaches() {
        clear(CacheConfig.FOLLOW_STATUS_CACHE);
        clear(CacheConfig.FOLLOW_COUNTS_CACHE);
        clear(CacheConfig.FOLLOWING_IDS_CACHE);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private String statusCacheKey(Long followerId, Long followingId) {
        return followerId + ":" + followingId;
    }
}
