package com.connectsphere.follow.util;

import com.connectsphere.follow.dto.FollowResponse;
import com.connectsphere.follow.entity.Follow;

public final class FollowMapper {

    private FollowMapper() {
    }

/**
 * Performs the to response operation.
 * @param follow method input parameter
 * @return resulting value
 */
    public static FollowResponse toResponse(Follow follow) {
        return new FollowResponse(
                follow.getFollowId(),
                follow.getFollowerId(),
                follow.getFollowingId(),
                follow.getCreatedAt()
        );
    }
}
