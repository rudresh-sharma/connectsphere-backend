package com.connectsphere.post.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.post.client.AuthServiceClient;
import com.connectsphere.post.client.CreateNotificationRequest;
import com.connectsphere.post.client.FollowServiceClient;
import com.connectsphere.post.client.IndexPostRequest;
import com.connectsphere.post.client.NotificationServiceClient;
import com.connectsphere.post.client.NotificationType;
import com.connectsphere.post.client.SearchServiceClient;
import com.connectsphere.post.client.UserSummary;
import com.connectsphere.post.config.CacheConfig;
import com.connectsphere.post.dto.AdminModeratePostRequest;
import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.CreatePromotionOrderRequest;
import com.connectsphere.post.dto.PostCounterRequest;
import com.connectsphere.post.dto.PostReportRequest;
import com.connectsphere.post.dto.PostReportResponse;
import com.connectsphere.post.dto.PostResponse;
import com.connectsphere.post.dto.ResolvePostReportRequest;
import com.connectsphere.post.dto.UpdatePostRequest;
import com.connectsphere.post.dto.VerifyPromotionPaymentRequest;
import com.connectsphere.post.entity.Bookmark;
import com.connectsphere.post.entity.ModerationStatus;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.entity.PostReport;
import com.connectsphere.post.entity.PostType;
import com.connectsphere.post.entity.PostVisibility;
import com.connectsphere.post.exception.BadRequestException;
import com.connectsphere.post.exception.ForbiddenException;
import com.connectsphere.post.messaging.NotificationEventPublisher;
import com.connectsphere.post.repository.BookmarkRepository;
import com.connectsphere.post.repository.PostReportRepository;
import com.connectsphere.post.repository.PostRepository;
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

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private FollowServiceClient followServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private SearchServiceClient searchServiceClient;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private PostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostServiceImpl(
                bookmarkRepository,
                postRepository,
                postReportRepository,
                authServiceClient,
                followServiceClient,
                notificationServiceClient,
                searchServiceClient,
                notificationEventPublisher,
                mediaStorageService,
                new ObjectMapper(),
                cacheManager
        );
        ReflectionTestUtils.setField(service, "defaultPromotionDurationDays", 7);
        ReflectionTestUtils.setField(service, "defaultPromotionAmountPaise", 9900);
        ReflectionTestUtils.setField(service, "razorpayKeyId", "");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "");
    }

    @Test
    void createPostSavesFlaggedPostIndexesItAndPublishesMention() {
        CreatePostRequest request = new CreatePostRequest(
                1L,
                "This scam post mentions @friend",
                List.of("https://cdn.test/image.jpg"),
                null,
                null
        );
        Post saved = post(10L, 1L, request.content(), PostType.IMAGE, PostVisibility.PUBLIC);
        saved.setAutomatedFlagged(true);
        saved.setModerationStatus(ModerationStatus.FLAGGED);
        saved.setModerationReason("Automatically flagged for keyword: scam");

        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));
        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(authServiceClient.searchUsers("friend")).thenReturn(List.of(user(2L, "friend", "USER")));
        PostResponse response = service.createPost(request);

        assertEquals(10L, response.postId());
        assertEquals(ModerationStatus.FLAGGED, response.moderationStatus());
        verify(searchServiceClient).indexPost(any(IndexPostRequest.class));
        verify(notificationEventPublisher).publish(argThat(notification ->
                notification.recipientId().equals(2L)
                        && notification.actorId().equals(1L)
                        && notification.type() == NotificationType.MENTION));
    }

    @Test
    void getPublicPostsUsesViewerSpecificQueryWhenViewerPresent() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(10L, 2L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findPublicFeedExcludingAuthor(PostVisibility.PUBLIC, 5L, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "author", "USER"));

        Page<PostResponse> result = service.getPublicPosts(5L, pageable);

        assertEquals(1, result.getContent().size());
        verify(postRepository).findPublicFeedExcludingAuthor(PostVisibility.PUBLIC, 5L, pageable);
    }

    @Test
    void getFeedFallsBackToPublicForMissingUser() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable))
                .thenReturn(Page.empty(pageable));

        service.getFeed(null, pageable);

        verify(postRepository).findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable);
    }

    @Test
    void getFeedUsesNoFolloweeQueryWhenFollowListEmpty() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(followServiceClient.getFollowingIds(3L)).thenReturn(List.of());
        when(postRepository.findFeedPostsWithoutFollowees(3L, pageable))
                .thenReturn(Page.empty(pageable));

        service.getFeed(3L, pageable);

        verify(postRepository).findFeedPostsWithoutFollowees(3L, pageable);
    }

    @Test
    void getFeedUsesFolloweeQueryWhenFollowListPresent() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(followServiceClient.getFollowingIds(3L)).thenReturn(List.of(4L, 5L));
        when(postRepository.findFeedPostsWithFollowees(3L, List.of(4L, 5L), pageable))
                .thenReturn(Page.empty(pageable));

        service.getFeed(3L, pageable);

        verify(postRepository).findFeedPostsWithFollowees(3L, List.of(4L, 5L), pageable);
    }

    @Test
    void updatePostUpdatesContentReindexesAndOnlyPublishesNewMentions() {
        Post post = post(11L, 1L, "hello @old", PostType.TEXT, PostVisibility.PUBLIC);
        UpdatePostRequest request = new UpdatePostRequest("updated @old and @new", List.of(), null, PostVisibility.FOLLOWERS_ONLY);
        Post saved = post(11L, 1L, "updated @old and @new", PostType.TEXT, PostVisibility.FOLLOWERS_ONLY);

        when(postRepository.findById(11L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));
        when(authServiceClient.searchUsers("new")).thenReturn(List.of(user(2L, "new", "USER")));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.updatePost(11L, 1L, request);

        assertEquals("updated @old and @new", response.content());
        assertEquals(PostVisibility.FOLLOWERS_ONLY, response.visibility());
        verify(searchServiceClient).removePostIndex(11L);
        verify(searchServiceClient).indexPost(any(IndexPostRequest.class));
        verify(notificationEventPublisher).publish(argThat(notification -> notification.recipientId().equals(2L)));
    }

    @Test
    void deletePostMarksDeletedCleansBookmarksAndIndex() {
        Post post = post(11L, 1L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        service.deletePost(11L, 1L);

        assertTrue(post.isDeleted());
        verify(bookmarkRepository).deleteByPostId(11L);
        verify(searchServiceClient).removePostIndex(11L);
        verify(cache).evictIfPresent(11L);
    }

    @Test
    void updateCounterClampsNegativeValuesAtZero() {
        Post post = post(12L, 1L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        post.setLikesCount(1);
        when(postRepository.findById(12L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.updateCounter(12L, new PostCounterRequest("likes", -5));

        assertEquals(0L, response.likesCount());
    }

    @Test
    void updateCounterRejectsUnknownCounterType() {
        Post post = post(12L, 1L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findById(12L)).thenReturn(Optional.of(post));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateCounter(12L, new PostCounterRequest("views", 1)));

        assertEquals("counterType must be likes, comments, or shares", ex.getMessage());
    }

    @Test
    void sharePostRequiresRequesterId() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.sharePost(1L, null));

        assertEquals("requesterId is required", ex.getMessage());
    }

    @Test
    void createPromotionOrderFailsWhenRazorpayNotConfigured() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createPromotionOrder(21L, new CreatePromotionOrderRequest(1L, 1000, 7)));

        assertEquals("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.", ex.getMessage());
    }

    @Test
    void verifyPromotionPaymentRejectsMismatchedOrderId() {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        Post post = post(22L, 1L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        post.setPromotionOrderId("order_real");
        when(postRepository.findById(22L)).thenReturn(Optional.of(post));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verifyPromotionPayment(22L, new VerifyPromotionPaymentRequest(1L, "order_other", "pay_1", "sig")));

        assertEquals("Payment order does not match this post", ex.getMessage());
    }

    @Test
    void addBookmarkRejectsPrivatePostForDifferentUser() {
        Post post = post(13L, 1L, "private", PostType.TEXT, PostVisibility.PRIVATE);
        when(postRepository.findById(13L)).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () -> service.addBookmark(13L, 5L));
    }

    @Test
    void addBookmarkSkipsSaveWhenAlreadyBookmarked() {
        Post post = post(13L, 1L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findById(13L)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserIdAndPostId(5L, 13L)).thenReturn(true);

        service.addBookmark(13L, 5L);

        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void getBookmarkedPostsReturnsInBookmarkOrder() {
        Post second = post(30L, 2L, "second", PostType.TEXT, PostVisibility.PUBLIC);
        Post first = post(20L, 1L, "first", PostType.TEXT, PostVisibility.PUBLIC);
        when(bookmarkRepository.findPostIdsByUserIdOrderByCreatedAtDesc(9L)).thenReturn(List.of(20L, 30L));
        when(postRepository.findByPostIdInAndDeletedFalse(List.of(20L, 30L))).thenReturn(List.of(second, first));
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "first-user", "USER"));
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "second-user", "USER"));

        List<PostResponse> responses = service.getBookmarkedPosts(9L);

        assertEquals(List.of(20L, 30L), responses.stream().map(PostResponse::postId).toList());
    }

    @Test
    void deletePostsByAuthorDeletesMediaBookmarksAndSearchIndex() {
        Post first = post(40L, 7L, "one", PostType.IMAGE, PostVisibility.PUBLIC);
        first.setMediaUrls(List.of("u1", "u2"));
        Post second = post(41L, 7L, "two", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findByAuthorId(7L)).thenReturn(List.of(first, second));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        service.deletePostsByAuthor(7L);

        verify(bookmarkRepository).deleteByUserId(7L);
        verify(bookmarkRepository).deleteByPostIdIn(List.of(40L, 41L));
        verify(mediaStorageService).deletePostMediaByUrl("u1");
        verify(mediaStorageService).deletePostMediaByUrl("u2");
        verify(searchServiceClient).removePostIndex(40L);
        verify(searchServiceClient).removePostIndex(41L);
        verify(postRepository).deleteAll(List.of(first, second));
    }

    @Test
    void adminDeletePostMarksPostRemovedAndResolvesOpenReports() {
        Post post = post(50L, 3L, "bad", PostType.TEXT, PostVisibility.PUBLIC);
        PostReport report = PostReport.builder().reportId(1L).postId(50L).reporterId(9L).resolved(false).build();
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postRepository.findById(50L)).thenReturn(Optional.of(post));
        when(postReportRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of(report));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        service.adminDeletePost(50L, 99L);

        assertTrue(post.isDeleted());
        assertEquals(ModerationStatus.REMOVED, post.getModerationStatus());
        assertTrue(report.isResolved());
        assertEquals("Post removed by admin", report.getResolutionNote());
        verify(bookmarkRepository).deleteByPostId(50L);
        verify(searchServiceClient).removePostIndex(50L);
    }

    @Test
    void approvePromotionActivatesPendingPromotionAndPublishesNotification() {
        Post post = post(60L, 3L, "promo", PostType.TEXT, PostVisibility.PUBLIC);
        post.setPromotionStatus("PENDING_APPROVAL");
        post.setPromotionDurationDays(2);
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postRepository.findById(60L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(3L)).thenReturn(user(3L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.approvePromotion(60L, 99L);

        assertTrue(response.promoted());
        assertEquals("ACTIVE", response.promotionStatus());
        verify(notificationEventPublisher).publish(argThat(notification ->
                notification.recipientId().equals(3L) && notification.type() == NotificationType.SYSTEM));
    }

    @Test
    void rejectPromotionWithoutCapturedPaymentMarksRejected() {
        Post post = post(61L, 3L, "promo", PostType.TEXT, PostVisibility.PUBLIC);
        post.setPromotionStatus("PENDING");
        post.setPromotionPaymentId(null);
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postRepository.findById(61L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(3L)).thenReturn(user(3L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.rejectPromotion(61L, 99L);

        assertFalse(response.promoted());
        assertEquals("REJECTED", response.promotionStatus());
        verify(notificationEventPublisher).publish(any(CreateNotificationRequest.class));
    }

    @Test
    void moderatePostRemovedDeletesBookmarksAndIndex() {
        Post post = post(70L, 5L, "remove", PostType.TEXT, PostVisibility.PUBLIC);
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postRepository.findById(70L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(5L)).thenReturn(user(5L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.moderatePost(
                70L,
                new AdminModeratePostRequest(99L, ModerationStatus.REMOVED, "bad")
        );

        assertEquals(ModerationStatus.REMOVED, response.moderationStatus());
        verify(bookmarkRepository).deleteByPostId(70L);
        verify(searchServiceClient).removePostIndex(70L);
    }

    @Test
    void reportPostTrimsReasonAndReturnsReporterName() {
        Post post = post(80L, 3L, "post", PostType.TEXT, PostVisibility.PUBLIC);
        PostReport saved = PostReport.builder()
                .reportId(10L)
                .postId(80L)
                .reporterId(7L)
                .reason("spam")
                .resolved(false)
                .build();
        when(postRepository.findById(80L)).thenReturn(Optional.of(post));
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "reporter", "USER"));
        when(postReportRepository.save(any(PostReport.class))).thenReturn(saved);

        PostReportResponse response = service.reportPost(80L, new PostReportRequest(7L, " spam "));

        assertEquals(10L, response.reportId());
        assertEquals("reporter", response.reporterUsername());
    }

    @Test
    void resolveReportCanAlsoRemoveUnderlyingPost() {
        PostReport report = PostReport.builder().reportId(15L).postId(81L).reporterId(7L).resolved(false).build();
        Post post = post(81L, 3L, "bad", PostType.TEXT, PostVisibility.PUBLIC);
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postReportRepository.findById(15L)).thenReturn(Optional.of(report));
        when(postRepository.findById(81L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(postReportRepository.save(report)).thenReturn(report);
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "reporter", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostReportResponse response = service.resolveReport(15L, new ResolvePostReportRequest(99L, true, "removed"));

        assertTrue(report.isResolved());
        assertTrue(post.isDeleted());
        assertEquals("removed", response.resolutionNote());
        verify(bookmarkRepository).deleteByPostId(81L);
        verify(searchServiceClient).removePostIndex(81L);
    }

    private Post post(Long postId, Long authorId, String content, PostType postType, PostVisibility visibility) {
        return Post.builder()
                .postId(postId)
                .authorId(authorId)
                .content(content)
                .mediaUrls(List.of())
                .postType(postType)
                .visibility(visibility)
                .moderationStatus(ModerationStatus.APPROVED)
                .promotionStatus("NONE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private UserSummary user(Long userId, String username, String role) {
        return new UserSummary(userId, username, "Full " + username, "pic", username + "@mail.com", role, true);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
