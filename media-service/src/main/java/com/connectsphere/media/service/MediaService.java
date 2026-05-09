package com.connectsphere.media.service;

import com.connectsphere.media.dto.MediaResponse;
import com.connectsphere.media.dto.ReelResponse;
import com.connectsphere.media.dto.StoryResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines Media business operations.
 */
public interface MediaService {
    MediaResponse uploadMedia(Long uploaderId, Long linkedPostId, MultipartFile file);
    List<MediaResponse> getMediaByPost(Long postId);
    MediaResponse getMediaById(Long mediaId);
    void deleteMedia(Long mediaId);

    StoryResponse createStory(Long authorId, String caption, MultipartFile file);
    List<StoryResponse> getActiveStories(Long viewerId);
    StoryResponse viewStory(Long storyId, Long viewerId);
    void deleteStory(Long storyId);
    List<StoryResponse> getStoriesByUser(Long userId, Long viewerId);
    int expireOldStories();

    ReelResponse createReel(Long authorId, String caption, MultipartFile file);
    List<ReelResponse> getActiveReels();
    ReelResponse viewReel(Long reelId, Long viewerId);
    void deleteReel(Long reelId);
    List<ReelResponse> getReelsByUser(Long userId);
    void deleteUserContent(Long userId);
}
