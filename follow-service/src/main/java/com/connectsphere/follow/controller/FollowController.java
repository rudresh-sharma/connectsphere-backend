package com.connectsphere.follow.controller;

import com.connectsphere.follow.dto.FollowCountsResponse;
import com.connectsphere.follow.dto.FollowRequest;
import com.connectsphere.follow.dto.FollowResponse;
import com.connectsphere.follow.dto.FollowStatusResponse;
import com.connectsphere.follow.dto.SuggestedUserResponse;
import com.connectsphere.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
/**
 * Exposes Follow API endpoints.
 */


@RestController
@RequestMapping("/follows")

public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }
/**
 * Handles the follow request.
 * @param followingId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @PostMapping("/{followingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowResponse follow(
            @PathVariable("followingId") Long followingId,
            @Valid @RequestBody FollowRequest request
    ) {
        return followService.follow(request.followerId(), followingId);
    }
/**
 * Handles the unfollow request.
 * @param followingId entity identifier
 * @param followerId entity identifier
 */

    @DeleteMapping("/{followingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(
            @PathVariable("followingId") Long followingId,
            @RequestParam("followerId") Long followerId
    ) {
        followService.unfollow(followerId, followingId);
    }
/**
 * Handles the following request.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/following/{userId}")
    public Page<FollowResponse> following(
            @PathVariable("userId") Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return followService.getFollowing(userId, pageable);
    }
/**
 * Handles the followers request.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/followers/{userId}")
    public Page<FollowResponse> followers(
            @PathVariable("userId") Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return followService.getFollowers(userId, pageable);
    }
/**
 * Handles the status request.
 * @param followingId entity identifier
 * @param followerId entity identifier
 * @return resulting value
 */

    @GetMapping("/status/{followingId}")
    public FollowStatusResponse status(
            @PathVariable("followingId") Long followingId,
            @RequestParam("followerId") Long followerId
    ) {
        return followService.getStatus(followerId, followingId);
    }
/**
 * Handles the s request.
 * @param userId entity identifier
 * @return resulting value
 */

    @GetMapping("/counts/{userId}")
    public FollowCountsResponse counts(@PathVariable("userId") Long userId) {
        return followService.getCounts(userId);
    }
/**
 * Handles the following ids request.
 * @param userId entity identifier
 * @return matching results
 */

    @GetMapping("/following-ids/{userId}")
    public List<Long> followingIds(@PathVariable("userId") Long userId) {
        return followService.getFollowingIds(userId);
    }
/**
 * Handles the mutual following ids request.
 * @param otherUserId entity identifier
 * @param userId entity identifier
 * @return matching results
 */

    @GetMapping("/mutual/{otherUserId}")
    public List<Long> mutualFollowingIds(
            @PathVariable("otherUserId") Long otherUserId,
            @RequestParam("userId") Long userId
    ) {
        return followService.getMutualFollowingIds(userId, otherUserId);
    }
/**
 * Handles the suggested users request.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/suggestions/{userId}")
    public Page<SuggestedUserResponse> suggestedUsers(
            @PathVariable("userId") Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return followService.getSuggestedUsers(userId, pageable);
    }
/**
 * Deletes user relationships.
 * @param userId entity identifier
 */

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRelationships(@PathVariable("userId") Long userId) {
        followService.deleteUserRelationships(userId);
    }
}
