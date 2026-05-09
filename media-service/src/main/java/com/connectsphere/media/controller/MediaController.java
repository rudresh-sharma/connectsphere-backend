package com.connectsphere.media.controller;

import com.connectsphere.media.dto.MediaResponse;
import com.connectsphere.media.dto.ReelResponse;
import com.connectsphere.media.dto.StoryResponse;
import com.connectsphere.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
/**
 * Exposes Media API endpoints.
 */


@RestController
@RequiredArgsConstructor

public class MediaController {

    private final MediaService mediaService;

    // ---- Media endpoints ----
/**
 * Uploads media.
 * @param uploaderId entity identifier
 * @param "linkedPostId" method input parameter
 * @param linkedPostId entity identifier
 * @param file uploaded file
 * @return operation result
 */
    @PostMapping("/media/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse uploadMedia(@RequestParam("uploaderId") Long uploaderId,
                                     @RequestParam(value = "linkedPostId", required = false) Long linkedPostId,
                                     @RequestParam("file") MultipartFile file) {
        return mediaService.uploadMedia(uploaderId, linkedPostId, file);
    }
/**
 * Returns media by post.
 * @param postId entity identifier
 * @return matching results
 */

    @GetMapping("/media/post/{postId}")
    public List<MediaResponse> getMediaByPost(@PathVariable("postId") Long postId) {
        return mediaService.getMediaByPost(postId);
    }
/**
 * Returns media by id.
 * @param mediaId entity identifier
 * @return operation result
 */

    @GetMapping("/media/{mediaId}")
    public MediaResponse getMediaById(@PathVariable("mediaId") Long mediaId) {
        return mediaService.getMediaById(mediaId);
    }
/**
 * Deletes media.
 * @param mediaId entity identifier
 */

    @DeleteMapping("/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable("mediaId") Long mediaId) {
        mediaService.deleteMedia(mediaId);
    }

    // ---- Story endpoints ----
/**
 * Creates story.
 * @param authorId entity identifier
 * @param "caption" method input parameter
 * @param caption method input parameter
 * @param file uploaded file
 * @return operation result
 */
    @PostMapping("/stories")
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse createStory(@RequestParam("authorId") Long authorId,
                                     @RequestParam(value = "caption", required = false) String caption,
                                     @RequestParam("file") MultipartFile file) {
        return mediaService.createStory(authorId, caption, file);
    }
/**
 * Returns active stories.
 * @param "viewerId" method input parameter
 * @param viewerId entity identifier
 * @return matching results
 */

    @GetMapping("/stories")
    public List<StoryResponse> getActiveStories(@RequestParam(value = "viewerId", required = false) Long viewerId) {
        return mediaService.getActiveStories(viewerId);
    }
/**
 * Handles the view story request.
 * @param storyId entity identifier
 * @param "viewerId" method input parameter
 * @param viewerId entity identifier
 * @return resulting value
 */

    @PostMapping("/stories/{storyId}/view")
    public StoryResponse viewStory(@PathVariable("storyId") Long storyId,
                                   @RequestParam(value = "viewerId", required = false) Long viewerId) {
        return mediaService.viewStory(storyId, viewerId);
    }
/**
 * Deletes story.
 * @param storyId entity identifier
 */

    @DeleteMapping("/stories/{storyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStory(@PathVariable("storyId") Long storyId) {
        mediaService.deleteStory(storyId);
    }
/**
 * Returns stories by user.
 * @param userId entity identifier
 * @param "viewerId" method input parameter
 * @param viewerId entity identifier
 * @return matching results
 */

    @GetMapping("/stories/user/{userId}")
    public List<StoryResponse> getStoriesByUser(@PathVariable("userId") Long userId,
                                                @RequestParam(value = "viewerId", required = false) Long viewerId) {
        return mediaService.getStoriesByUser(userId, viewerId);
    }

    // ---- Reel endpoints ----
/**
 * Creates reel.
 * @param authorId entity identifier
 * @param "caption" method input parameter
 * @param caption method input parameter
 * @param file uploaded file
 * @return operation result
 */
    @PostMapping("/reels")
    @ResponseStatus(HttpStatus.CREATED)
    public ReelResponse createReel(@RequestParam("authorId") Long authorId,
                                   @RequestParam(value = "caption", required = false) String caption,
                                   @RequestParam("file") MultipartFile file) {
        return mediaService.createReel(authorId, caption, file);
    }
/**
 * Returns active reels.
 * @return matching results
 */

    @GetMapping("/reels")
    public List<ReelResponse> getActiveReels() {
        return mediaService.getActiveReels();
    }
/**
 * Handles the view reel request.
 * @param reelId entity identifier
 * @param "viewerId" method input parameter
 * @param viewerId entity identifier
 * @return resulting value
 */

    @PostMapping("/reels/{reelId}/view")
    public ReelResponse viewReel(@PathVariable("reelId") Long reelId,
                                 @RequestParam(value = "viewerId", required = false) Long viewerId) {
        return mediaService.viewReel(reelId, viewerId);
    }
/**
 * Deletes reel.
 * @param reelId entity identifier
 */

    @DeleteMapping("/reels/{reelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReel(@PathVariable("reelId") Long reelId) {
        mediaService.deleteReel(reelId);
    }
/**
 * Returns reels by user.
 * @param userId entity identifier
 * @return matching results
 */

    @GetMapping("/reels/user/{userId}")
    public List<ReelResponse> getReelsByUser(@PathVariable("userId") Long userId) {
        return mediaService.getReelsByUser(userId);
    }
/**
 * Deletes user content.
 * @param userId entity identifier
 */

    @DeleteMapping("/media/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserContent(@PathVariable("userId") Long userId) {
        mediaService.deleteUserContent(userId);
    }
}
