package com.connectsphere.like.service;

import com.connectsphere.like.dto.ChangeReactionRequest;
import com.connectsphere.like.dto.LikeRequest;
import com.connectsphere.like.dto.LikeResponse;
import com.connectsphere.like.dto.ReactionSummary;
import com.connectsphere.like.entity.TargetType;
import java.util.List;

/**
 * Defines Like business operations.
 */
public interface LikeService {

    LikeResponse likeTarget(LikeRequest request);

    void unlikeTarget(Long userId, Long targetId, TargetType targetType);

    boolean hasLiked(Long userId, Long targetId, TargetType targetType);

    List<LikeResponse> getLikesByTarget(Long targetId, TargetType targetType);

    List<LikeResponse> getLikesByUser(Long userId);

    long getLikeCount(Long targetId, TargetType targetType);

    ReactionSummary getReactionSummary(Long targetId, TargetType targetType);

    LikeResponse changeReaction(Long userId, Long targetId, TargetType targetType, ChangeReactionRequest request);

    void deleteLikesByUser(Long userId);
}
