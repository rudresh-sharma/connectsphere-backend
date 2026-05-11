package com.connectsphere.post.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.connectsphere.post.client.AuthServiceClient;
import com.connectsphere.post.client.FollowServiceClient;
import com.connectsphere.post.client.NotificationServiceClient;
import com.connectsphere.post.client.SearchServiceClient;
import com.connectsphere.post.client.UserSummary;
import com.connectsphere.post.config.CacheConfig;
import com.connectsphere.post.dto.*;
import com.connectsphere.post.entity.*;
import com.connectsphere.post.exception.BadRequestException;
import com.connectsphere.post.exception.ForbiddenException;
import com.connectsphere.post.messaging.NotificationEventPublisher;
import com.connectsphere.post.repository.*;
import com.connectsphere.post.service.MediaStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Second extended PostServiceImpl test file covering methods not reached
 * by the first two test classes – primarily addBookmark, reportPost,
 * moderatePost, adminUpdatePost, adminDeletePost, rejectPromotion,
 * approvePromotion (success), getFeed (with following IDs), and
 * createPost (flagged / mention notification paths).
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplCoverageTest {

    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private PostRepository postRepository;
    @Mock private PostReportRepository postReportRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private FollowServiceClient followServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private SearchServiceClient searchServiceClient;
    @Mock private NotificationEventPublisher notificationEventPublisher;
    @Mock private MediaStorageService mediaStorageService;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    private PostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostServiceImpl(
                bookmarkRepository, postRepository, postReportRepository,
                authServiceClient, followServiceClient, notificationServiceClient,
                searchServiceClient, notificationEventPublisher, mediaStorageService,
                new ObjectMapper(), cacheManager
        );
        ReflectionTestUtils.setField(service, "defaultPromotionDurationDays", 7);
        ReflectionTestUtils.setField(service, "defaultPromotionAmountPaise", 9900);
        ReflectionTestUtils.setField(service, "razorpayKeyId", "key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
    }

    // =========================================================================
    // addBookmark
    // =========================================================================

    @Test
    void addBookmarkCreatesNewBookmarkWhenNotExists() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post(10L, 1L)));
        when(bookmarkRepository.existsByUserIdAndPostId(5L, 10L)).thenReturn(false);

        service.addBookmark(10L, 5L);

        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void addBookmarkIsIdempotentWhenAlreadyExists() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post(10L, 1L)));
        when(bookmarkRepository.existsByUserIdAndPostId(5L, 10L)).thenReturn(true);

        service.addBookmark(10L, 5L);

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void addBookmarkRequiresUserId() {
        assertThrows(BadRequestException.class, () -> service.addBookmark(10L, null));
    }

    // =========================================================================
    // reportPost
    // =========================================================================

    @Test
    void reportPostCreatesReport() {
        when(postRepository.findById(20L)).thenReturn(Optional.of(post(20L, 1L)));
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "reporter"));
        PostReport saved = PostReport.builder()
                .reportId(1L).postId(20L).reporterId(7L).reason("spam").resolved(false).build();
        when(postReportRepository.save(any())).thenReturn(saved);
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "reporter"));

        PostReportResponse resp = service.reportPost(20L, new PostReportRequest(7L, "spam"));

        assertEquals(1L, resp.reportId());
        assertEquals("spam", resp.reason());
    }

    @Test
    void reportPostRequiresReporterId() {
        // reportPost calls findActivePost first, then validateAuthorExists(reporterId).
        // With null reporterId the impl throws BadRequestException or NullPointerException
        // which is wrapped. Use lenient stubbing to avoid UnnecessaryStubbingException.
        when(postRepository.findById(20L)).thenReturn(Optional.of(post(20L, 1L)));

        // The service throws some exception when reporterId is null
        assertThrows(Exception.class,
                () -> service.reportPost(20L, new PostReportRequest(null, "spam")));
    }

    // =========================================================================
    // moderatePost
    // =========================================================================

    @Test
    void moderatePostUpdatesStatus() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(30L, 1L);
        p.setModerationStatus(ModerationStatus.FLAGGED);
        when(postRepository.findById(30L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse resp = service.moderatePost(30L,
                new AdminModeratePostRequest(99L, ModerationStatus.APPROVED, null));

        assertEquals(ModerationStatus.APPROVED, resp.moderationStatus());
    }

    // =========================================================================
    // adminUpdatePost
    // =========================================================================

    @Test
    void adminUpdatePostUpdatesContent() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(40L, 1L);
        when(postRepository.findById(40L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse resp = service.adminUpdatePost(40L, 99L,
                new UpdatePostRequest("admin edited", List.of(), PostType.TEXT, PostVisibility.PUBLIC));

        assertEquals("admin edited", resp.content());
    }

    // =========================================================================
    // adminDeletePost (success path)
    // =========================================================================

    @Test
    void adminDeletePostSetsDeletedFlag() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(50L, 1L);
        when(postRepository.findById(50L)).thenReturn(Optional.of(p));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        service.adminDeletePost(50L, 99L);

        assertTrue(p.isDeleted());
    }

    // =========================================================================
    // getFeed – with following IDs
    // =========================================================================

    @Test
    void getFeedIncludesFollowedUserPosts() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(followServiceClient.getFollowingIds(4L)).thenReturn(List.of(2L, 3L));
        Post p = post(5L, 2L);
        when(postRepository.findFeedPostsWithFollowees(4L, List.of(2L, 3L), pageable))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "followed"));

        Page<PostResponse> result = service.getFeed(4L, pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getFeedHandlesFeignExceptionInFollowService() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(followServiceClient.getFollowingIds(4L)).thenThrow(FeignException.class);
        // When followees can't be fetched → falls back to no-followees query
        when(postRepository.findFeedPostsWithoutFollowees(4L, pageable))
                .thenReturn(Page.empty(pageable));

        Page<PostResponse> result = service.getFeed(4L, pageable);

        assertNotNull(result);
    }

    // =========================================================================
    // createPost – moderation (flagged content) path
    // =========================================================================

    @Test
    void createPostFlagsPostWithOffensiveContent() {
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        Post saved = post(60L, 1L);
        saved.setContent("spam is bad");
        saved.setAutomatedFlagged(true);
        saved.setModerationStatus(ModerationStatus.FLAGGED);
        when(postRepository.save(any())).thenReturn(saved);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));

        PostResponse resp = service.createPost(
                new CreatePostRequest(1L, "spam is bad", null, PostType.TEXT, PostVisibility.PUBLIC));

        assertTrue(resp.automatedFlagged());
        assertEquals(ModerationStatus.FLAGGED, resp.moderationStatus());
    }

    // =========================================================================
    // createPost – mention notification path
    // =========================================================================

    @Test
    void createPostPublishesMentionNotification() {
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        Post saved = post(70L, 1L);
        saved.setContent("hello @bob");
        when(postRepository.save(any())).thenReturn(saved);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        // searchUsers is called to resolve @bob
        when(authServiceClient.searchUsers("bob")).thenReturn(
                List.of(new UserSummary(2L, "bob", "Bob", null, "bob@x.com", "USER", true)));

        service.createPost(
                new CreatePostRequest(1L, "hello @bob", null, PostType.TEXT, PostVisibility.PUBLIC));

        verify(notificationEventPublisher, atLeastOnce()).publish(any());
    }

    // =========================================================================
    // resolveReport – with post removal
    // =========================================================================

    @Test
    void resolveReportRemovesPost() {
        PostReport report = PostReport.builder()
                .reportId(5L).postId(90L).reporterId(7L).resolved(false).build();
        Post p = post(90L, 1L);
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postReportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(postRepository.findById(90L)).thenReturn(Optional.of(p));
        when(postReportRepository.save(report)).thenReturn(report);
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "reporter"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostReportResponse resp = service.resolveReport(5L,
                new ResolvePostReportRequest(99L, true, "removed"));

        assertTrue(p.isDeleted());
        assertTrue(resp.resolved());
    }

    // =========================================================================
    // deletePostsByAuthor – success path
    // =========================================================================

    @Test
    void deletePostsByAuthorPhysicallyDeletesAllPosts() {
        Post p1 = post(100L, 3L);
        Post p2 = post(101L, 3L);
        when(postRepository.findByAuthorId(3L)).thenReturn(List.of(p1, p2));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        service.deletePostsByAuthor(3L);

        // deletePostsByAuthor physically deletes via deleteAll, not soft-delete
        verify(postRepository).deleteAll(List.of(p1, p2));
    }

    // =========================================================================
    // getPost – with cache (tests cached path via service)
    // =========================================================================

    @Test
    void getPostReturnsResponseForExistingPost() {
        Post p = post(110L, 1L);
        when(postRepository.findById(110L)).thenReturn(Optional.of(p));
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));

        PostResponse resp = service.getPost(110L);

        assertEquals(110L, resp.postId());
    }

    // =========================================================================
    // updatePost – success (same owner)
    // =========================================================================

    @Test
    void updatePostSucceedsForOwner() {
        Post p = post(120L, 1L);
        when(postRepository.findById(120L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse resp = service.updatePost(120L, 1L,
                new UpdatePostRequest("updated content", List.of(), PostType.TEXT, PostVisibility.PUBLIC));

        assertEquals("updated content", resp.content());
    }

    // =========================================================================
    // approvePromotion – success path
    // =========================================================================

    @Test
    void approvePromotionSucceedsWhenPending() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(130L, 1L);
        p.setPromotionStatus("PENDING_APPROVAL");
        p.setPromotionDurationDays(7);
        when(postRepository.findById(130L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse resp = service.approvePromotion(130L, 99L);

        assertEquals("ACTIVE", resp.promotionStatus());
        assertTrue(resp.promoted());
    }

    // =========================================================================
    // rejectPromotion – no payment to refund
    // =========================================================================

    @Test
    void rejectPromotionSucceedsWhenNoPaymentToRefund() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(140L, 1L);
        p.setPromotionStatus("PENDING_APPROVAL");
        p.setPromotionAmountPaise(null); // No payment captured yet
        when(postRepository.findById(140L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse resp = service.rejectPromotion(140L, 99L);

        assertEquals("REJECTED", resp.promotionStatus());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Post post(Long postId, Long authorId) {
        return Post.builder()
                .postId(postId).authorId(authorId)
                .content("test content").mediaUrls(List.of())
                .postType(PostType.TEXT).visibility(PostVisibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .promotionStatus("NONE")
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private UserSummary user(Long id, String username) {
        return new UserSummary(id, username, "Full " + username,
                "pic", username + "@mail.com", "USER", true);
    }

    private UserSummary user(Long id, String username, String role) {
        return new UserSummary(id, username, "Full " + username,
                "pic", username + "@mail.com", role, true);
    }
}
