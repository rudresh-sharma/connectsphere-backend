package com.connectsphere.follow.service;

import com.connectsphere.follow.dto.FollowCountsResponse;
import com.connectsphere.follow.dto.FollowResponse;
import com.connectsphere.follow.dto.FollowStatusResponse;
import com.connectsphere.follow.dto.SuggestedUserResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines Follow business operations.
 */
public interface FollowService {

    FollowResponse follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    Page<FollowResponse> getFollowing(Long userId, Pageable pageable);

    Page<FollowResponse> getFollowers(Long userId, Pageable pageable);

    FollowStatusResponse getStatus(Long followerId, Long followingId);

    FollowCountsResponse getCounts(Long userId);

    List<Long> getFollowingIds(Long userId);

    List<Long> getMutualFollowingIds(Long userId, Long otherUserId);

    Page<SuggestedUserResponse> getSuggestedUsers(Long userId, Pageable pageable);

    void deleteUserRelationships(Long userId);
}
