package com.connectsphere.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectsphere.media.client.AuthServiceClient;
import com.connectsphere.media.client.FollowServiceClient;
import com.connectsphere.media.client.UserSummary;
import com.connectsphere.media.config.CloudinaryProperties;
import com.connectsphere.media.dto.MediaResponse;
import com.connectsphere.media.dto.ReelResponse;
import com.connectsphere.media.dto.StoryResponse;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.MediaType;
import com.connectsphere.media.entity.Reel;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.exception.BadRequestException;
import com.connectsphere.media.exception.ResourceNotFoundException;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.ReelRepository;
import com.connectsphere.media.repository.StoryRepository;
import com.connectsphere.media.service.ImageModerationService;
import com.connectsphere.media.service.MediaService;
import feign.FeignException;
import java.time.Instant;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
/**
 * Implements Media business operations.
 */


@Service
@RequiredArgsConstructor
@Transactional

public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);
    private final MediaRepository mediaRepository;
    private final StoryRepository storyRepository;
    private final ReelRepository reelRepository;
    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final ImageModerationService imageModerationService;
    private final AuthServiceClient authServiceClient;
    private final FollowServiceClient followServiceClient;
/**
 * Uploads media.
 * @param uploaderId entity identifier
 * @param linkedPostId entity identifier
 * @param file uploaded file
 * @return operation result
 */

    @Override
    public MediaResponse uploadMedia(Long uploaderId, Long linkedPostId, MultipartFile file) {
        String contentType = file.getContentType();
        MediaType mediaType = resolveMediaType(contentType);
        String resourceType = mediaType == MediaType.VIDEO ? "video" : "image";
        imageModerationService.assertSafe(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", cloudinaryProperties.getFolder(), "resource_type", resourceType));
            String url = (String) result.get("secure_url");
            long sizeKb = file.getSize() / 1024;

            Media media = Media.builder()
                    .uploaderId(uploaderId).url(url).mediaType(mediaType)
                    .sizeKb(sizeKb).mimeType(contentType).linkedPostId(linkedPostId)
                    .build();
            return toMediaResponse(mediaRepository.save(media));
        } catch (Exception ex) {
            throw new BadRequestException("Could not upload media to Cloudinary: " + ex.getMessage());
        }
    }
/**
 * Returns media by post.
 * @param postId entity identifier
 * @return matching results
 */

    @Override @Transactional(readOnly = true)
    public List<MediaResponse> getMediaByPost(Long postId) {
        return mediaRepository.findByLinkedPostIdAndDeletedFalse(postId).stream().map(this::toMediaResponse).toList();
    }
/**
 * Returns media by id.
 * @param mediaId entity identifier
 * @return operation result
 */

    @Override @Transactional(readOnly = true)
    public MediaResponse getMediaById(Long mediaId) {
        return toMediaResponse(mediaRepository.findById(mediaId).filter(m -> !m.isDeleted()).orElseThrow(() -> new ResourceNotFoundException("Media not found")));
    }
/**
 * Deletes media.
 * @param mediaId entity identifier
 */

    @Override
    public void deleteMedia(Long mediaId) {
        Media media = mediaRepository.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("Media not found"));
        media.setDeleted(true);
        mediaRepository.save(media);
    }
/**
 * Creates story.
 * @param authorId entity identifier
 * @param caption method input parameter
 * @param file uploaded file
 * @return operation result
 */

    @Override
    public StoryResponse createStory(Long authorId, String caption, MultipartFile file) {
        String contentType = file.getContentType();
        MediaType mediaType = resolveMediaType(contentType);
        String resourceType = mediaType == MediaType.VIDEO ? "video" : "image";
        imageModerationService.assertSafe(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", cloudinaryProperties.getFolder() + "/stories", "resource_type", resourceType));
            String url = (String) result.get("secure_url");

            Story story = Story.builder()
                    .authorId(authorId).mediaUrl(url).caption(caption).mediaType(mediaType)
                    .build();
            return toStoryResponse(storyRepository.save(story), authorId);
        } catch (Exception ex) {
            throw new BadRequestException("Could not upload story media: " + ex.getMessage());
        }
    }
/**
 * Returns active stories.
 * @param viewerId entity identifier
 * @return matching results
 */

    @Override @Transactional(readOnly = true)
    public List<StoryResponse> getActiveStories(Long viewerId) {
        if (viewerId == null || viewerId <= 0) {
            return List.of();
        }

        List<Long> visibleAuthorIds = new ArrayList<>();
        visibleAuthorIds.add(viewerId);

        try {
            List<Long> followingIds = followServiceClient.getFollowingIds(viewerId);
            if (followingIds != null && !followingIds.isEmpty()) {
                visibleAuthorIds.addAll(followingIds);
            }
        } catch (FeignException ex) {
            log.warn("Could not fetch following IDs for viewer {}", viewerId);
        }

        return storyRepository.findByAuthorIdInAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(visibleAuthorIds, Instant.now())
                .stream().map(story -> toStoryResponse(story, viewerId)).toList();
    }
/**
 * Performs the view story operation.
 * @param storyId entity identifier
 * @param viewerId entity identifier
 * @return resulting value
 */

    @Override
    public StoryResponse viewStory(Long storyId, Long viewerId) {
        Story story = storyRepository.findById(storyId).filter(Story::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getAuthorId().equals(viewerId)) {
            story.setViewsCount(story.getViewsCount() + 1);
            story = storyRepository.save(story);
        }
        return toStoryResponse(story, viewerId);
    }
/**
 * Deletes story.
 * @param storyId entity identifier
 */

    @Override
    public void deleteStory(Long storyId) {
        Story story = storyRepository.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        story.setActive(false);
        storyRepository.save(story);
    }
/**
 * Returns stories by user.
 * @param userId entity identifier
 * @param viewerId entity identifier
 * @return matching results
 */

    @Override @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesByUser(Long userId, Long viewerId) {
        return storyRepository.findByAuthorIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now())
                .stream().map(story -> toStoryResponse(story, viewerId)).toList();
    }
/**
 * Performs the expire old stories operation.
 * @return resulting value
 */

    @Override
    @Scheduled(fixedRate = 300_000)
    public int expireOldStories() {
        int expired = storyRepository.expireOldStories(Instant.now());
        if (expired > 0) log.info("Expired {} stories", expired);
        return expired;
    }
/**
 * Creates reel.
 * @param authorId entity identifier
 * @param caption method input parameter
 * @param file uploaded file
 * @return operation result
 */

    @Override
    public ReelResponse createReel(Long authorId, String caption, MultipartFile file) {
        validateVideoOnly(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", cloudinaryProperties.getFolder() + "/reels", "resource_type", "video")
            );
            String url = (String) result.get("secure_url");

            Reel reel = Reel.builder()
                    .authorId(authorId)
                    .videoUrl(url)
                    .caption(caption)
                    .build();

            return toReelResponse(reelRepository.save(reel));
        } catch (Exception ex) {
            throw new BadRequestException("Could not upload reel video: " + ex.getMessage());
        }
    }
/**
 * Returns active reels.
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<ReelResponse> getActiveReels() {
        return reelRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toReelResponse)
                .toList();
    }
/**
 * Performs the view reel operation.
 * @param reelId entity identifier
 * @param viewerId entity identifier
 * @return resulting value
 */

    @Override
    public ReelResponse viewReel(Long reelId, Long viewerId) {
        Reel reel = reelRepository.findById(reelId)
                .filter(Reel::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

        if (viewerId != null && viewerId > 0 && !reel.getAuthorId().equals(viewerId)) {
            reel.setViewsCount(reel.getViewsCount() + 1);
            reel = reelRepository.save(reel);
        }

        return toReelResponse(reel);
    }
/**
 * Deletes reel.
 * @param reelId entity identifier
 */

    @Override
    public void deleteReel(Long reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        reel.setActive(false);
        reelRepository.save(reel);
    }
/**
 * Returns reels by user.
 * @param userId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<ReelResponse> getReelsByUser(Long userId) {
        return reelRepository.findByAuthorIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toReelResponse)
                .toList();
    }
/**
 * Deletes user content.
 * @param userId entity identifier
 */

    @Override
    public void deleteUserContent(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }

        List<Story> stories = storyRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
        for (Story story : stories) {
            deleteUploadedAssetByUrl(story.getMediaUrl(), story.getMediaType() == MediaType.VIDEO ? "video" : "image");
        }
        storyRepository.deleteAll(stories);

        List<Reel> reels = reelRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
        for (Reel reel : reels) {
            deleteUploadedAssetByUrl(reel.getVideoUrl(), "video");
        }
        reelRepository.deleteAll(reels);
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) return MediaType.VIDEO;
        return MediaType.IMAGE;
    }

    private void deleteUploadedAssetByUrl(String mediaUrl, String resourceType) {
        String publicId = extractPublicId(mediaUrl);
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception ex) {
            log.warn("Could not delete Cloudinary asset for publicId={}", publicId, ex);
        }
    }

    private String extractPublicId(String mediaUrl) {
        try {
            List<String> segments = new ArrayList<>();
            for (String segment : URI.create(mediaUrl).getPath().split("/")) {
                if (!segment.isBlank()) {
                    segments.add(segment);
                }
            }

            int uploadIndex = segments.indexOf("upload");
            if (uploadIndex < 0 || uploadIndex + 1 >= segments.size()) {
                return null;
            }

            int publicIdStart = uploadIndex + 1;
            for (int i = uploadIndex + 1; i < segments.size(); i++) {
                if (segments.get(i).matches("v\\d+")) {
                    publicIdStart = i + 1;
                    break;
                }
            }

            if (publicIdStart >= segments.size()) {
                return null;
            }

            List<String> publicIdSegments = new ArrayList<>(segments.subList(publicIdStart, segments.size()));
            String lastSegment = publicIdSegments.get(publicIdSegments.size() - 1);
            int extensionIndex = lastSegment.lastIndexOf('.');
            if (extensionIndex > 0) {
                publicIdSegments.set(publicIdSegments.size() - 1, lastSegment.substring(0, extensionIndex));
            }

            return String.join("/", publicIdSegments);
        } catch (Exception ex) {
            return null;
        }
    }

    private void validateVideoOnly(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BadRequestException("Only video uploads are allowed for reels");
        }
    }

    private MediaResponse toMediaResponse(Media m) {
        return new MediaResponse(m.getMediaId(), m.getUploaderId(), m.getUrl(), m.getMediaType(), m.getSizeKb(), m.getMimeType(), m.getLinkedPostId(), m.getUploadedAt());
    }

    private StoryResponse toStoryResponse(Story s, Long viewerId) {
        UserSummary user = getUserSummary(s.getAuthorId(), "story " + s.getStoryId());
        long visibleViewsCount = s.getAuthorId().equals(viewerId) ? s.getViewsCount() : 0;
        return new StoryResponse(
                s.getStoryId(),
                s.getAuthorId(),
                s.getMediaUrl(),
                s.getCaption(),
                s.getMediaType(),
                visibleViewsCount,
                s.getExpiresAt(),
                s.getCreatedAt(),
                user != null ? user.username() : null,
                user != null ? user.profilePicUrl() : null
        );
    }

    private ReelResponse toReelResponse(Reel reel) {
        UserSummary user = getUserSummary(reel.getAuthorId(), "reel " + reel.getReelId());
        return new ReelResponse(
                reel.getReelId(),
                reel.getAuthorId(),
                reel.getVideoUrl(),
                reel.getCaption(),
                reel.getViewsCount(),
                reel.getCreatedAt(),
                user != null ? user.username() : null,
                user != null ? user.fullName() : null,
                user != null ? user.profilePicUrl() : null
        );
    }

    private UserSummary getUserSummary(Long userId, String logContext) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (FeignException ex) {
            log.warn("Could not fetch author for {}", logContext);
            return null;
        }
    }
}
