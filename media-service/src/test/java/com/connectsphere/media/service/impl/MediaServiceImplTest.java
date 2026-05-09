package com.connectsphere.media.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
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
import feign.FeignException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private ReelRepository reelRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private CloudinaryProperties cloudinaryProperties;

    @Mock
    private ImageModerationService imageModerationService;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private FollowServiceClient followServiceClient;

    private MediaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MediaServiceImpl(
                mediaRepository,
                storyRepository,
                reelRepository,
                cloudinary,
                cloudinaryProperties,
                imageModerationService,
                authServiceClient,
                followServiceClient
        );
    }

    @Test
    void uploadMediaStoresImageResponseWhenCloudinarySucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[2048]);
        Media saved = Media.builder()
                .mediaId(10L)
                .uploaderId(3L)
                .url("https://cdn.test/photo.jpg")
                .mediaType(MediaType.IMAGE)
                .sizeKb(2)
                .mimeType("image/jpeg")
                .linkedPostId(99L)
                .build();

        when(cloudinary.uploader()).thenReturn(uploader);
        when(cloudinaryProperties.getFolder()).thenReturn("connectsphere");
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.test/photo.jpg"));
        when(mediaRepository.save(any(Media.class))).thenReturn(saved);

        MediaResponse response = service.uploadMedia(3L, 99L, file);

        assertEquals(10L, response.mediaId());
        assertEquals("https://cdn.test/photo.jpg", response.url());
        assertEquals(MediaType.IMAGE, response.mediaType());
        verify(imageModerationService).assertSafe(file);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void uploadMediaWrapsCloudinaryFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[10]);

        when(cloudinary.uploader()).thenReturn(uploader);
        when(cloudinaryProperties.getFolder()).thenReturn("connectsphere");
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new RuntimeException("down"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(1L, 5L, file));

        assertEquals("Could not upload media to Cloudinary: down", ex.getMessage());
    }

    @Test
    void getMediaByPostReturnsMappedResults() {
        Media media = Media.builder()
                .mediaId(1L)
                .uploaderId(2L)
                .url("https://cdn.test/image.jpg")
                .mediaType(MediaType.IMAGE)
                .sizeKb(10)
                .mimeType("image/jpeg")
                .linkedPostId(88L)
                .build();
        when(mediaRepository.findByLinkedPostIdAndDeletedFalse(88L)).thenReturn(List.of(media));

        List<MediaResponse> result = service.getMediaByPost(88L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).mediaId());
    }

    @Test
    void getMediaByIdReturnsMediaWhenNotDeleted() {
        Media media = Media.builder().mediaId(4L).uploaderId(1L).url("u").mediaType(MediaType.IMAGE).build();
        when(mediaRepository.findById(4L)).thenReturn(Optional.of(media));

        MediaResponse result = service.getMediaById(4L);

        assertEquals(4L, result.mediaId());
    }

    @Test
    void getMediaByIdRejectsDeletedMedia() {
        Media media = Media.builder().mediaId(4L).uploaderId(1L).url("u").mediaType(MediaType.IMAGE).deleted(true).build();
        when(mediaRepository.findById(4L)).thenReturn(Optional.of(media));

        assertThrows(ResourceNotFoundException.class, () -> service.getMediaById(4L));
    }

    @Test
    void deleteMediaMarksExistingMediaDeleted() {
        Media media = Media.builder().mediaId(4L).uploaderId(1L).url("https://cdn.test/image.jpg").mediaType(MediaType.IMAGE).build();
        when(mediaRepository.findById(4L)).thenReturn(Optional.of(media));

        service.deleteMedia(4L);

        assertEquals(true, media.isDeleted());
        verify(mediaRepository).save(media);
    }

    @Test
    void createStoryStoresStoryAndIncludesAuthorSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "story.jpg", "image/jpeg", new byte[] {1, 2});
        Story saved = Story.builder()
                .storyId(6L)
                .authorId(9L)
                .mediaUrl("https://cdn.test/story.jpg")
                .caption("hello")
                .mediaType(MediaType.IMAGE)
                .viewsCount(0)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .active(true)
                .build();

        when(cloudinary.uploader()).thenReturn(uploader);
        when(cloudinaryProperties.getFolder()).thenReturn("connectsphere");
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.test/story.jpg"));
        when(storyRepository.save(any(Story.class))).thenReturn(saved);
        when(authServiceClient.getUserById(9L)).thenReturn(userSummary(9L, "story-user"));

        StoryResponse response = service.createStory(9L, "hello", file);

        assertEquals(6L, response.storyId());
        assertEquals("story-user", response.authorUsername());
        verify(imageModerationService).assertSafe(file);
    }

    @Test
    void createStoryWrapsUploadFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "story.jpg", "image/jpeg", new byte[] {1});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(cloudinaryProperties.getFolder()).thenReturn("connectsphere");
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new RuntimeException("failed"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createStory(1L, "x", file));

        assertEquals("Could not upload story media: failed", ex.getMessage());
    }

    @Test
    void getActiveStoriesReturnsEmptyWhenViewerIdMissing() {
        assertEquals(List.of(), service.getActiveStories(null));
        assertEquals(List.of(), service.getActiveStories(0L));
    }

    @Test
    void getActiveStoriesUsesViewerAndFollowingIdsAndFallsBackWhenAuthorLookupFails() {
        Story story = Story.builder()
                .storyId(8L)
                .authorId(7L)
                .mediaUrl("https://cdn.test/story.jpg")
                .mediaType(MediaType.IMAGE)
                .viewsCount(4)
                .expiresAt(Instant.now().plusSeconds(120))
                .createdAt(Instant.now())
                .active(true)
                .build();

        when(followServiceClient.getFollowingIds(3L)).thenReturn(List.of(7L, 11L));
        when(storyRepository.findByAuthorIdInAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(any(List.class), any(Instant.class)))
                .thenReturn(List.of(story));
        when(authServiceClient.getUserById(7L)).thenThrow(mock(FeignException.class));

        List<StoryResponse> responses = service.getActiveStories(3L);

        assertEquals(1, responses.size());
        assertNull(responses.get(0).authorUsername());
        assertEquals(0L, responses.get(0).viewsCount());
        verify(followServiceClient).getFollowingIds(3L);
    }

    @Test
    void getActiveStoriesContinuesWhenFollowingLookupFails() {
        Story story = Story.builder()
                .storyId(8L)
                .authorId(3L)
                .mediaUrl("https://cdn.test/story.jpg")
                .mediaType(MediaType.IMAGE)
                .viewsCount(4)
                .expiresAt(Instant.now().plusSeconds(120))
                .createdAt(Instant.now())
                .active(true)
                .build();

        when(followServiceClient.getFollowingIds(3L)).thenThrow(mock(FeignException.class));
        when(storyRepository.findByAuthorIdInAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(any(List.class), any(Instant.class)))
                .thenReturn(List.of(story));
        when(authServiceClient.getUserById(3L)).thenReturn(userSummary(3L, "owner"));

        List<StoryResponse> responses = service.getActiveStories(3L);

        assertEquals(1, responses.size());
        assertEquals(4L, responses.get(0).viewsCount());
    }

    @Test
    void viewStoryIncrementsViewCountForNonOwner() {
        Story story = Story.builder()
                .storyId(2L)
                .authorId(1L)
                .mediaUrl("https://cdn.test/story.jpg")
                .mediaType(MediaType.IMAGE)
                .active(true)
                .build();
        when(storyRepository.findById(2L)).thenReturn(Optional.of(story));
        when(storyRepository.save(story)).thenReturn(story);
        when(authServiceClient.getUserById(1L)).thenReturn(userSummary(1L, "owner"));

        StoryResponse response = service.viewStory(2L, 9L);

        assertEquals(1L, story.getViewsCount());
        assertEquals(0L, response.viewsCount());
        verify(storyRepository).save(story);
    }

    @Test
    void viewStoryDoesNotIncrementViewCountForOwner() {
        Story story = Story.builder()
                .storyId(2L)
                .authorId(1L)
                .mediaUrl("https://cdn.test/story.jpg")
                .mediaType(MediaType.IMAGE)
                .viewsCount(5)
                .active(true)
                .build();
        when(storyRepository.findById(2L)).thenReturn(Optional.of(story));
        when(authServiceClient.getUserById(1L)).thenReturn(userSummary(1L, "owner"));

        StoryResponse response = service.viewStory(2L, 1L);

        assertEquals(5L, response.viewsCount());
        verify(storyRepository, never()).save(any(Story.class));
    }

    @Test
    void deleteStoryMarksStoryInactive() {
        Story story = Story.builder().storyId(3L).authorId(1L).mediaUrl("x").mediaType(MediaType.IMAGE).active(true).build();
        when(storyRepository.findById(3L)).thenReturn(Optional.of(story));

        service.deleteStory(3L);

        assertEquals(false, story.isActive());
        verify(storyRepository).save(story);
    }

    @Test
    void getStoriesByUserReturnsMappedStories() {
        Story story = Story.builder()
                .storyId(5L)
                .authorId(6L)
                .mediaUrl("https://cdn.test/story.jpg")
                .caption("cap")
                .mediaType(MediaType.IMAGE)
                .viewsCount(3)
                .expiresAt(Instant.now().plusSeconds(60))
                .createdAt(Instant.now())
                .active(true)
                .build();
        when(storyRepository.findByAuthorIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(eq(6L), any(Instant.class)))
                .thenReturn(List.of(story));
        when(authServiceClient.getUserById(6L)).thenReturn(userSummary(6L, "author"));

        List<StoryResponse> responses = service.getStoriesByUser(6L, 6L);

        assertEquals(1, responses.size());
        assertEquals("author", responses.get(0).authorUsername());
        assertEquals(3L, responses.get(0).viewsCount());
    }

    @Test
    void expireOldStoriesReturnsExpiredCount() {
        when(storyRepository.expireOldStories(any(Instant.class))).thenReturn(2);

        assertEquals(2, service.expireOldStories());
    }

    @Test
    void createReelRejectsNonVideoUploads() {
        MockMultipartFile image = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] { 1 });

        assertThrows(BadRequestException.class, () -> service.createReel(1L, "caption", image));
    }

    @Test
    void createReelStoresReelAndMapsAuthor() throws Exception {
        MockMultipartFile video = new MockMultipartFile("file", "reel.mp4", "video/mp4", new byte[] {1, 2});
        Reel saved = Reel.builder()
                .reelId(7L)
                .authorId(2L)
                .videoUrl("https://cdn.test/reel.mp4")
                .caption("caption")
                .viewsCount(4)
                .createdAt(Instant.now())
                .active(true)
                .build();

        when(cloudinary.uploader()).thenReturn(uploader);
        when(cloudinaryProperties.getFolder()).thenReturn("connectsphere");
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cdn.test/reel.mp4"));
        when(reelRepository.save(any(Reel.class))).thenReturn(saved);
        when(authServiceClient.getUserById(2L)).thenReturn(userSummary(2L, "reel-user"));

        ReelResponse response = service.createReel(2L, "caption", video);

        assertEquals(7L, response.reelId());
        assertEquals("reel-user", response.authorUsername());
    }

    @Test
    void getActiveReelsMapsReelsAndHandlesMissingAuthor() {
        Reel reel = Reel.builder()
                .reelId(7L)
                .authorId(1L)
                .videoUrl("https://cdn.test/reel.mp4")
                .caption("caption")
                .viewsCount(2)
                .createdAt(Instant.now())
                .active(true)
                .build();
        when(reelRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(reel));
        when(authServiceClient.getUserById(1L)).thenThrow(mock(FeignException.class));

        List<ReelResponse> responses = service.getActiveReels();

        assertEquals(1, responses.size());
        assertNull(responses.get(0).authorUsername());
    }

    @Test
    void viewReelIncrementsViewCountForOtherViewer() {
        Reel reel = Reel.builder().reelId(7L).authorId(1L).videoUrl("https://cdn.test/reel.mp4").active(true).build();
        when(reelRepository.findById(7L)).thenReturn(Optional.of(reel));
        when(reelRepository.save(reel)).thenReturn(reel);
        when(authServiceClient.getUserById(1L)).thenReturn(userSummary(1L, "owner"));

        ReelResponse response = service.viewReel(7L, 2L);

        assertEquals(1L, reel.getViewsCount());
        assertNotNull(response);
        verify(reelRepository).save(reel);
    }

    @Test
    void viewReelSkipsIncrementForOwnerOrMissingViewer() {
        Reel reel = Reel.builder().reelId(7L).authorId(1L).videoUrl("https://cdn.test/reel.mp4").active(true).build();
        when(reelRepository.findById(7L)).thenReturn(Optional.of(reel));
        when(authServiceClient.getUserById(1L)).thenReturn(userSummary(1L, "owner"));

        service.viewReel(7L, 1L);
        service.viewReel(7L, null);

        verify(reelRepository, never()).save(any(Reel.class));
    }

    @Test
    void deleteReelMarksInactive() {
        Reel reel = Reel.builder().reelId(5L).authorId(1L).videoUrl("x").active(true).build();
        when(reelRepository.findById(5L)).thenReturn(Optional.of(reel));

        service.deleteReel(5L);

        assertEquals(false, reel.isActive());
        verify(reelRepository).save(reel);
    }

    @Test
    void getReelsByUserReturnsMappedResults() {
        Reel reel = Reel.builder()
                .reelId(5L)
                .authorId(1L)
                .videoUrl("https://cdn.test/reel.mp4")
                .caption("caption")
                .viewsCount(8)
                .createdAt(Instant.now())
                .active(true)
                .build();
        when(reelRepository.findByAuthorIdAndActiveTrueOrderByCreatedAtDesc(1L)).thenReturn(List.of(reel));
        when(authServiceClient.getUserById(1L)).thenReturn(userSummary(1L, "owner"));

        List<ReelResponse> results = service.getReelsByUser(1L);

        assertEquals(1, results.size());
        assertEquals("owner", results.get(0).authorUsername());
    }

    @Test
    void deleteUserContentRequiresUserId() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.deleteUserContent(null));

        assertEquals("userId is required", ex.getMessage());
    }

    @Test
    void deleteUserContentDeletesStoriesAndReelsAndSwallowsCloudinaryErrors() throws Exception {
        Story validStory = Story.builder()
                .storyId(1L)
                .authorId(5L)
                .mediaUrl("https://res.cloudinary.com/demo/image/upload/v123/connectsphere/stories/s1.jpg")
                .mediaType(MediaType.IMAGE)
                .build();
        Story invalidStory = Story.builder()
                .storyId(2L)
                .authorId(5L)
                .mediaUrl("not-a-valid-url")
                .mediaType(MediaType.IMAGE)
                .build();
        Reel reel = Reel.builder()
                .reelId(3L)
                .authorId(5L)
                .videoUrl("https://res.cloudinary.com/demo/video/upload/v999/connectsphere/reels/r1.mp4")
                .build();

        when(storyRepository.findByAuthorIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(validStory, invalidStory));
        when(reelRepository.findByAuthorIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(reel));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("connectsphere/stories/s1"), any(Map.class))).thenThrow(new RuntimeException("ignore"));

        assertDoesNotThrow(() -> service.deleteUserContent(5L));

        verify(uploader).destroy(eq("connectsphere/stories/s1"), any(Map.class));
        verify(uploader).destroy(eq("connectsphere/reels/r1"), any(Map.class));
        verify(storyRepository).deleteAll(List.of(validStory, invalidStory));
        verify(reelRepository).deleteAll(List.of(reel));
    }

    private UserSummary userSummary(Long userId, String username) {
        return new UserSummary(userId, username, "Full " + username, username + "@mail.com", "https://cdn.test/" + username + ".jpg");
    }
}
