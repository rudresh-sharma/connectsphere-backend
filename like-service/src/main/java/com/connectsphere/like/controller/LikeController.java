package com.connectsphere.like.controller;

import com.connectsphere.like.dto.ChangeReactionRequest;
import com.connectsphere.like.dto.LikeRequest;
import com.connectsphere.like.dto.LikeResponse;
import com.connectsphere.like.dto.ReactionSummary;
import com.connectsphere.like.entity.TargetType;
import com.connectsphere.like.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
/**
 * Exposes Like API endpoints.
 */


@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")

public class LikeController {

    private final LikeService likeService;
/**
 * Handles the like target request.
 * @param request request payload
 * @return resulting value
 */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LikeResponse likeTarget(@Valid @RequestBody LikeRequest request) {
        return likeService.likeTarget(request);
    }
/**
 * Handles the unlike target request.
 * @param userId entity identifier
 * @param targetId entity identifier
 * @param targetType method input parameter
 */

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikeTarget(@RequestParam("userId") Long userId,
                             @RequestParam("targetId") Long targetId,
                             @RequestParam("targetType") TargetType targetType) {
        likeService.unlikeTarget(userId, targetId, targetType);
    }
/**
 * Handles the has liked request.
 * @param userId entity identifier
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return true when the condition is satisfied; otherwise false
 */

    @GetMapping("/check")
    public boolean hasLiked(@RequestParam("userId") Long userId,
                            @RequestParam("targetId") Long targetId,
                            @RequestParam("targetType") TargetType targetType) {
        return likeService.hasLiked(userId, targetId, targetType);
    }
/**
 * Returns likes by target.
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return matching results
 */

    @GetMapping("/target/{targetId}")
    public List<LikeResponse> getLikesByTarget(@PathVariable("targetId") Long targetId,
                                               @RequestParam("targetType") TargetType targetType) {
        return likeService.getLikesByTarget(targetId, targetType);
    }
/**
 * Returns likes by user.
 * @param userId entity identifier
 * @return matching results
 */

    @GetMapping("/user/{userId}")
    public List<LikeResponse> getLikesByUser(@PathVariable("userId") Long userId) {
        return likeService.getLikesByUser(userId);
    }
/**
 * Returns like count.
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return operation result
 */

    @GetMapping("/count/{targetId}")
    public long getLikeCount(@PathVariable("targetId") Long targetId,
                             @RequestParam("targetType") TargetType targetType) {
        return likeService.getLikeCount(targetId, targetType);
    }
/**
 * Returns reaction summary.
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @return operation result
 */

    @GetMapping("/summary/{targetId}")
    public ReactionSummary getReactionSummary(@PathVariable("targetId") Long targetId,
                                              @RequestParam("targetType") TargetType targetType) {
        return likeService.getReactionSummary(targetId, targetType);
    }
/**
 * Changes reaction.
 * @param userId entity identifier
 * @param targetId entity identifier
 * @param targetType method input parameter
 * @param request request payload
 * @return resulting value
 */

    @PutMapping
    public LikeResponse changeReaction(@RequestParam("userId") Long userId,
                                       @RequestParam("targetId") Long targetId,
                                       @RequestParam("targetType") TargetType targetType,
                                       @Valid @RequestBody ChangeReactionRequest request) {
        return likeService.changeReaction(userId, targetId, targetType, request);
    }
/**
 * Deletes likes by user.
 * @param userId entity identifier
 */

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLikesByUser(@PathVariable("userId") Long userId) {
        likeService.deleteLikesByUser(userId);
    }
}
