package com.connectsphere.post.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.post.client.AuthServiceClient;
import com.connectsphere.post.client.FollowServiceClient;
import com.connectsphere.post.client.NotificationServiceClient;
import com.connectsphere.post.client.SearchServiceClient;
import com.connectsphere.post.client.UserSummary;
import com.connectsphere.post.config.CacheConfig;
import com.connectsphere.post.dto.PostCounterRequest;
import com.connectsphere.post.dto.PostReportRequest;
import com.connectsphere.post.dto.PostReportResponse;
import com.connectsphere.post.dto.PostResponse;
import com.connectsphere.post.dto.ResolvePostReportRequest;
import com.connectsphere.post.dto.UpdatePostRequest;
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

/**
 * Extended tests targeting uncovered branches in PostServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplExtendedTest {

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

    // -------------------------------------------------------------------------
    // getPublicPosts
    // -------------------------------------------------------------------------

    @Test
    void getPublicPostsNoViewerUsesPromotedFirstQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable))
                .thenReturn(Page.empty(pageable));

        Page<PostResponse> result = service.getPublicPosts(null, pageable);

        assertNotNull(result);
        verify(postRepository).findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable);
    }

    @Test
    void getPublicPostsViewerIdZeroUsesPromotedFirstQuery() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable))
                .thenReturn(Page.empty(pageable));

        service.getPublicPosts(0L, pageable);

        verify(postRepository).findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable);
    }

    // -------------------------------------------------------------------------
    // getUserPosts
    // -------------------------------------------------------------------------

    @Test
    void getUserPostsReturnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(1L, 5L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(authServiceClient.getUserById(5L)).thenReturn(user(5L, "author", "USER"));

        Page<PostResponse> result = service.getUserPosts(5L, pageable);

        assertEquals(1, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // searchPublicPosts
    // -------------------------------------------------------------------------

    @Test
    void searchPublicPostsWithBlankKeywordFallsBackToPublicFeed() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable))
                .thenReturn(Page.empty(pageable));

        service.searchPublicPosts("   ", pageable);

        verify(postRepository).findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable);
    }

    @Test
    void searchPublicPostsWithNullKeywordFallsBackToPublicFeed() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable))
                .thenReturn(Page.empty(pageable));

        service.searchPublicPosts(null, pageable);

        verify(postRepository).findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable);
    }

    @Test
    void searchPublicPostsWithKeywordQueriesDatabase() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(2L, 1L, "java spring", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.searchPublicPosts("java", pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));

        Page<PostResponse> result = service.searchPublicPosts("java", pageable);

        assertEquals(1, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // getPostsByVisibility
    // -------------------------------------------------------------------------

    @Test
    void getPostsByVisibilityReturnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(3L, 2L, "visible", PostType.TEXT, PostVisibility.FOLLOWERS_ONLY);
        when(postRepository.findByVisibilityAndDeletedFalseOrderByCreatedAtDesc(PostVisibility.FOLLOWERS_ONLY, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "author", "USER"));

        Page<PostResponse> result = service.getPostsByVisibility(PostVisibility.FOLLOWERS_ONLY, pageable);

        assertEquals(1, result.getContent().size());
    }

    // -------------------------------------------------------------------------
    // countActivePosts
    // -------------------------------------------------------------------------

    @Test
    void countActivePostsDelegatesToRepository() {
        when(postRepository.countByDeletedFalse()).thenReturn(42L);

        assertEquals(42L, service.countActivePosts());
    }

    // -------------------------------------------------------------------------
    // isBookmarked
    // -------------------------------------------------------------------------

    @Test
    void isBookmarkedReturnsFalseForNullUserId() {
        assertFalse(service.isBookmarked(1L, null));
        verify(bookmarkRepository, never()).existsByUserIdAndPostId(any(), any());
    }

    @Test
    void isBookmarkedReturnsFalseForZeroUserId() {
        assertFalse(service.isBookmarked(1L, 0L));
    }

    @Test
    void isBookmarkedReturnsTrueWhenExists() {
        when(bookmarkRepository.existsByUserIdAndPostId(3L, 5L)).thenReturn(true);

        assertTrue(service.isBookmarked(5L, 3L));
    }

    // -------------------------------------------------------------------------
    // removeBookmark
    // -------------------------------------------------------------------------

    @Test
    void removeBookmarkRequiresUserId() {
        assertThrows(BadRequestException.class, () -> service.removeBookmark(1L, null));
    }

    @Test
    void removeBookmarkRequiresPositiveUserId() {
        assertThrows(BadRequestException.class, () -> service.removeBookmark(1L, 0L));
    }

    @Test
    void removeBookmarkDeletesExistingBookmark() {
        com.connectsphere.post.entity.Bookmark bookmark = com.connectsphere.post.entity.Bookmark.builder()
                .userId(5L).postId(10L).build();
        when(bookmarkRepository.findByUserIdAndPostId(5L, 10L)).thenReturn(Optional.of(bookmark));

        service.removeBookmark(10L, 5L);

        verify(bookmarkRepository).delete(bookmark);
    }

    // -------------------------------------------------------------------------
    // getBookmarkedPosts – empty list path
    // -------------------------------------------------------------------------

    @Test
    void getBookmarkedPostsRequiresUserId() {
        assertThrows(BadRequestException.class, () -> service.getBookmarkedPosts(null));
    }

    @Test
    void getBookmarkedPostsReturnsEmptyWhenNoBookmarks() {
        when(bookmarkRepository.findPostIdsByUserIdOrderByCreatedAtDesc(8L)).thenReturn(List.of());

        List<PostResponse> result = service.getBookmarkedPosts(8L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getBookmarkedPostsReturnsEmptyWhenAllPostsDeleted() {
        when(bookmarkRepository.findPostIdsByUserIdOrderByCreatedAtDesc(8L)).thenReturn(List.of(99L));
        when(postRepository.findByPostIdInAndDeletedFalse(List.of(99L))).thenReturn(List.of());

        List<PostResponse> result = service.getBookmarkedPosts(8L);

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getAllPostsForAdmin
    // -------------------------------------------------------------------------

    @Test
    void getAllPostsForAdminReturnsMappedList() {
        Post post = post(10L, 1L, "admin visible", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));

        List<PostResponse> result = service.getAllPostsForAdmin();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).postId());
    }

    // -------------------------------------------------------------------------
    // getFlaggedPostsForAdmin
    // -------------------------------------------------------------------------

    @Test
    void getFlaggedPostsForAdminReturnsFlaggedPosts() {
        Post flagged = post(20L, 2L, "hate content", PostType.TEXT, PostVisibility.PUBLIC);
        flagged.setAutomatedFlagged(true);
        when(postRepository.findByAutomatedFlaggedTrueAndDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(flagged));
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "author", "USER"));

        List<PostResponse> result = service.getFlaggedPostsForAdmin();

        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // getPendingPromotionsForAdmin
    // -------------------------------------------------------------------------

    @Test
    void getPendingPromotionsForAdminReturnsPendingList() {
        Post pending = post(30L, 3L, "promo post", PostType.TEXT, PostVisibility.PUBLIC);
        pending.setPromotionStatus("PENDING_APPROVAL");
        when(postRepository.findByPromotionStatusAndDeletedFalseOrderByUpdatedAtDesc("PENDING_APPROVAL"))
                .thenReturn(List.of(pending));
        when(authServiceClient.getUserById(3L)).thenReturn(user(3L, "author", "USER"));

        List<PostResponse> result = service.getPendingPromotionsForAdmin();

        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // getReportsForAdmin
    // -------------------------------------------------------------------------

    @Test
    void getReportsForAdminReturnsMappedReports() {
        PostReport report = PostReport.builder()
                .reportId(1L).postId(5L).reporterId(9L).reason("spam").resolved(false).build();
        when(postReportRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of(report));
        when(authServiceClient.getUserById(9L)).thenReturn(user(9L, "reporter", "USER"));

        List<PostReportResponse> result = service.getReportsForAdmin(false);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).reportId());
    }

    // -------------------------------------------------------------------------
    // sharePost
    // -------------------------------------------------------------------------

    @Test
    void sharePostIncrementsSharesCount() {
        Post post = post(40L, 1L, "shareable", PostType.TEXT, PostVisibility.PUBLIC);
        post.setSharesCount(3);
        when(postRepository.findById(40L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.sharePost(40L, 2L);

        assertEquals(4L, response.sharesCount());
    }

    // -------------------------------------------------------------------------
    // updateCounter – comments / shares
    // -------------------------------------------------------------------------

    @Test
    void updateCounterIncrementsComments() {
        Post post = post(50L, 1L, "post", PostType.TEXT, PostVisibility.PUBLIC);
        post.setCommentsCount(2);
        when(postRepository.findById(50L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.updateCounter(50L, new PostCounterRequest("comments", 1));

        assertEquals(3L, response.commentsCount());
    }

    @Test
    void updateCounterIncrementsShares() {
        Post post = post(51L, 1L, "post", PostType.TEXT, PostVisibility.PUBLIC);
        post.setSharesCount(5);
        when(postRepository.findById(51L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author", "USER"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse response = service.updateCounter(51L, new PostCounterRequest("shares", 2));

        assertEquals(7L, response.sharesCount());
    }

    // -------------------------------------------------------------------------
    // deletePost – wrong owner
    // -------------------------------------------------------------------------

    @Test
    void deletePostRejectsDifferentOwner() {
        Post post = post(60L, 1L, "text", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findById(60L)).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () -> service.deletePost(60L, 99L));
    }

    // -------------------------------------------------------------------------
    // updatePost – wrong owner
    // -------------------------------------------------------------------------

    @Test
    void updatePostRejectsDifferentOwner() {
        Post post = post(70L, 1L, "original", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findById(70L)).thenReturn(Optional.of(post));
        UpdatePostRequest request = new UpdatePostRequest("updated", List.of(), null, null);

        assertThrows(ForbiddenException.class, () -> service.updatePost(70L, 99L, request));
    }

    // -------------------------------------------------------------------------
    // resolveReport – without removing post
    // -------------------------------------------------------------------------

    @Test
    void resolveReportWithoutRemovingPost() {
        PostReport report = PostReport.builder().reportId(20L).postId(90L).reporterId(7L).resolved(false).build();
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postReportRepository.findById(20L)).thenReturn(Optional.of(report));
        when(postReportRepository.save(report)).thenReturn(report);
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "reporter", "USER"));

        PostReportResponse response = service.resolveReport(20L, new ResolvePostReportRequest(99L, false, "noted"));

        assertTrue(response.resolved());
        assertEquals("noted", response.resolutionNote());
    }

    // -------------------------------------------------------------------------
    // assertAdmin – feign failure path
    // -------------------------------------------------------------------------

    @Test
    void assertAdminThrowsForbiddenWhenFeignFails() {
        // authServiceClient is called first in assertAdmin; findById is never reached
        when(authServiceClient.getUserById(99L))
                .thenThrow(FeignException.class);

        assertThrows(ForbiddenException.class, () -> service.adminDeletePost(80L, 99L));
    }

    // -------------------------------------------------------------------------
    // approvePromotion – not pending
    // -------------------------------------------------------------------------

    @Test
    void approvePromotionFailsWhenNotPending() {
        Post post = post(90L, 3L, "promo", PostType.TEXT, PostVisibility.PUBLIC);
        post.setPromotionStatus("ACTIVE");
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(postRepository.findById(90L)).thenReturn(Optional.of(post));

        assertThrows(BadRequestException.class, () -> service.approvePromotion(90L, 99L));
    }

    // -------------------------------------------------------------------------
    // deletePostsByAuthor – null authorId
    // -------------------------------------------------------------------------

    @Test
    void deletePostsByAuthorRejectsNullAuthorId() {
        assertThrows(BadRequestException.class, () -> service.deletePostsByAuthor(null));
    }

    // -------------------------------------------------------------------------
    // validateAuthorExists – null authorId
    // -------------------------------------------------------------------------

    @Test
    void createPostRejectsNullAuthorId() {
        com.connectsphere.post.dto.CreatePostRequest request =
                new com.connectsphere.post.dto.CreatePostRequest(null, "hello", null, null, null);

        assertThrows(BadRequestException.class, () -> service.createPost(request));
    }

    // -------------------------------------------------------------------------
    // cacheManager returns null cache
    // -------------------------------------------------------------------------

    @Test
    void evictPostCacheHandlesNullCacheGracefully() {
        Post post = post(100L, 1L, "hello", PostType.TEXT, PostVisibility.PUBLIC);
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(null);

        // Should not throw
        service.deletePost(100L, 1L);

        assertTrue(post.isDeleted());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
}
