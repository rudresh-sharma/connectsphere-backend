package com.connectsphere.like.service.impl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.like.client.PostServiceClient;
import com.connectsphere.like.config.CacheConfig;
import com.connectsphere.like.dto.ChangeReactionRequest;
import com.connectsphere.like.dto.LikeRequest;
import com.connectsphere.like.dto.LikeResponse;
import com.connectsphere.like.dto.ReactionSummary;
import com.connectsphere.like.entity.Like;
import com.connectsphere.like.entity.ReactionType;
import com.connectsphere.like.entity.TargetType;
import com.connectsphere.like.exception.BadRequestException;
import com.connectsphere.like.exception.ResourceNotFoundException;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.service.LikeService;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
/**
 * Implements Like business operations.
 */


@Service
@RequiredArgsConstructor
@Transactional

public class LikeServiceImpl implements LikeService {

	private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

	private final LikeRepository likeRepository;
	private final PostServiceClient postServiceClient;
	private final CacheManager cacheManager;
/**
 * Performs the like target operation.
 * @param request request payload
 * @return resulting value
 */

	@Override
	public LikeResponse likeTarget(LikeRequest request) {
		if (likeRepository.existsByUserIdAndTargetIdAndTargetType(request.userId(), request.targetId(),
				request.targetType())) {
			throw new BadRequestException("User has already reacted to this target");
		}

		Like like = Like.builder().userId(request.userId()).targetId(request.targetId())
				.targetType(request.targetType())
				.reactionType(request.reactionType() == null ? ReactionType.LIKE : request.reactionType()).build();

		Like saved = likeRepository.save(like);
		evictLikeCaches(saved.getUserId(), saved.getTargetId(), saved.getTargetType());

		if (request.targetType() == TargetType.POST) {
			incrementPostLikeCounter(request.targetId(), 1);
		}

		return toResponse(saved);
	}
/**
 * Performs the unlike target operation.
 * @param userId entity identifier
 * @param targetId entity identifier
 * @param targetType method input parameter
 */

	@Override
	public void unlikeTarget(Long userId, Long targetId, TargetType targetType) {
		Like like = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
				.orElseThrow(() -> new ResourceNotFoundException("Like not found"));

		likeRepository.delete(like);
		evictLikeCaches(userId, targetId, targetType);

		if (targetType == TargetType.POST) {
			incrementPostLikeCounter(targetId, -1);
		}
	}
/**
 * Performs the has liked operation.
 * @param userId entity identifier
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return true when the condition is satisfied; otherwise false
 */

	@Override
	@Transactional(readOnly = true)
	public boolean hasLiked(Long userId, Long targetId, TargetType targetType) {
		return likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
	}
/**
 * Returns likes by target.
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return matching results
 */

	@Override
	@Transactional(readOnly = true)
	public List<LikeResponse> getLikesByTarget(Long targetId, TargetType targetType) {
		return likeRepository.findByTargetIdAndTargetType(targetId, targetType).stream().map(this::toResponse).toList();
	}
/**
 * Returns likes by user.
 * @param userId entity identifier
 * @return matching results
 */

	@Override
	@Transactional(readOnly = true)
	public List<LikeResponse> getLikesByUser(Long userId) {
		return likeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
	}
/**
 * Returns like count.
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return operation result
 */

	@Override
	@Transactional(readOnly = true)
	public long getLikeCount(Long targetId, TargetType targetType) {
		return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
	}
/**
 * Returns reaction summary.
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return operation result
 */

	@Override
	@Transactional(readOnly = true)
	public ReactionSummary getReactionSummary(Long targetId, TargetType targetType) {
		List<Like> likes = likeRepository.findByTargetIdAndTargetType(targetId, targetType);
		Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
		for (Like like : likes) {
			counts.merge(like.getReactionType(), 1L, Long::sum);
		}
		return new ReactionSummary(targetId, likes.size(), counts);
	}
/**
 * Changes reaction.
 * @param userId entity identifier
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @param request request payload
 * @return resulting value
 */

	@Override
	public LikeResponse changeReaction(Long userId, Long targetId, TargetType targetType,
			ChangeReactionRequest request) {
		Like like = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
				.orElseThrow(() -> new ResourceNotFoundException("Like not found"));

		like.setReactionType(request.reactionType());
		Like saved = likeRepository.save(like);
		evictLikeCaches(saved.getUserId(), saved.getTargetId(), saved.getTargetType());
		return toResponse(saved);
	}
/**
 * Deletes likes by user.
 * @param userId entity identifier
 */

	@Override
	public void deleteLikesByUser(Long userId) {
		if (userId == null) {
			throw new BadRequestException("userId is required");
		}

		List<Like> userLikes = likeRepository.findByUserIdOrderByCreatedAtDesc(userId);
		likeRepository.deleteByUserId(userId);
		for (Like like : userLikes) {
			evictLikeCaches(like.getUserId(), like.getTargetId(), like.getTargetType());
		}
	}

	private void incrementPostLikeCounter(Long postId, long delta) {
		try {
			postServiceClient.updateCounter(postId, new PostServiceClient.CounterRequest("likes", delta));
		} catch (FeignException ex) {
			log.warn("Could not update like counter on post-service for postId={}: {}", postId, ex.getMessage());
		} catch (RuntimeException ex) {
			log.warn("Could not update like counter on post-service for postId={}", postId, ex);
		}
	}

	private LikeResponse toResponse(Like like) {
		return new LikeResponse(like.getLikeId(), like.getUserId(), like.getTargetId(), like.getTargetType(),
				like.getReactionType(), like.getCreatedAt());
	}

	private void evictLikeCaches(Long userId, Long targetId, TargetType targetType) {
		String targetKey = targetCacheKey(targetId, targetType);
		evict(CacheConfig.LIKE_COUNTS_CACHE, targetKey);
		evict(CacheConfig.REACTION_SUMMARIES_CACHE, targetKey);
		evict(CacheConfig.USER_REACTIONS_CACHE, userReactionCacheKey(userId, targetId, targetType));
	}

	private void evict(String cacheName, String key) {
		try {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache != null) {
				cache.evictIfPresent(key);
			}
		} catch (RuntimeException ex) {
			log.warn("Ignoring cache evict failure for cache={} key={}: {}", cacheName, key, ex.getMessage());
		}
	}

	private String targetCacheKey(Long targetId, TargetType targetType) {
		return targetType.name() + ":" + targetId;
	}

	private String userReactionCacheKey(Long userId, Long targetId, TargetType targetType) {
		return userId + ":" + targetCacheKey(targetId, targetType);
	}
}
