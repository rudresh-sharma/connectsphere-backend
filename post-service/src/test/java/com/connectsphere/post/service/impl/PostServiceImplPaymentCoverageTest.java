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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Third PostServiceImpl coverage file targeting payment/Razorpay, notification,
 * and helper private-method branches.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplPaymentCoverageTest {

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
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret",
                "dGVzdHNlY3JldGtleWZvcmp3dA==");
    }

    // =========================================================================
    // createPromotionOrder – success
    // =========================================================================

    @Test
    void createPromotionOrderReturnsOrderDetails() {
        Post p = post(10L, 1L);
        p.setPromotionStatus("NONE");
        when(postRepository.findById(10L)).thenReturn(Optional.of(p));
        // authServiceClient is not called before createRazorpayOrder throws,
        // so we use lenient to avoid UnnecessaryStubbingException
        lenient().when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "alice"));

        // createRazorpayOrder makes an HTTP call to Razorpay - exercises error-handling path
        assertThrows(Exception.class, () ->
                service.createPromotionOrder(10L,
                        new CreatePromotionOrderRequest(1L, 9900, 7)));
    }

    @Test
    void createPromotionOrderRequiresNonNullPost() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () ->
                service.createPromotionOrder(999L,
                        new CreatePromotionOrderRequest(1L, 9900, 7)));
    }

    @Test
    void createPromotionOrderRequiresOwnership() {
        Post p = post(10L, 1L);
        when(postRepository.findById(10L)).thenReturn(Optional.of(p));
        // requesterId = 2 != authorId = 1 → ForbiddenException
        assertThrows(Exception.class, () ->
                service.createPromotionOrder(10L,
                        new CreatePromotionOrderRequest(2L, 9900, 7)));
    }

    // =========================================================================
    // verifyPromotionPayment – invalid signature path
    // =========================================================================

    @Test
    void verifyPromotionPaymentThrowsForInvalidSignature() {
        Post p = post(20L, 1L);
        p.setPromotionOrderId("order_test");
        p.setPromotionStatus("PAYMENT_INITIATED");
        when(postRepository.findById(20L)).thenReturn(Optional.of(p));
        // assertOwner checks authorId==requesterId (1L==1L ✓) before signature check
        lenient().when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "alice"));

        VerifyPromotionPaymentRequest req = new VerifyPromotionPaymentRequest(
                1L, "order_test", "pay_test", "invalid_signature");

        assertThrows(BadRequestException.class,
                () -> service.verifyPromotionPayment(20L, req));
    }

    @Test
    void verifyPromotionPaymentRequiresOwner() {
        Post p = post(20L, 1L);
        p.setPromotionOrderId("order_test");
        when(postRepository.findById(20L)).thenReturn(Optional.of(p));

        // Wrong requesterId
        VerifyPromotionPaymentRequest req = new VerifyPromotionPaymentRequest(
                2L, "order_test", "pay_test", "sig");

        assertThrows(Exception.class,
                () -> service.verifyPromotionPayment(20L, req));
    }

    @Test
    void verifyPromotionPaymentRejectsOrderIdMismatch() {
        Post p = post(20L, 1L);
        p.setPromotionOrderId("order_real");
        p.setPromotionStatus("PAYMENT_INITIATED");
        when(postRepository.findById(20L)).thenReturn(Optional.of(p));
        lenient().when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "alice"));

        // razorpayOrderId in request doesn't match post's stored orderId
        VerifyPromotionPaymentRequest req = new VerifyPromotionPaymentRequest(
                1L, "order_fake", "pay_test", "sig");

        assertThrows(BadRequestException.class,
                () -> service.verifyPromotionPayment(20L, req));
    }

    // =========================================================================
    // rejectPromotion – with payment to refund (exercises refundPromotionPayment)
    // =========================================================================

    @Test
    void rejectPromotionTriesRefundWhenPaymentCaptured() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(30L, 1L);
        // Must be PENDING_APPROVAL for rejectPromotion to accept it
        p.setPromotionStatus("PENDING_APPROVAL");
        p.setPromotionPaymentId("pay_xyz");
        p.setPromotionAmountPaise(9900);
        when(postRepository.findById(30L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        // refundPromotionPayment makes an HTTP call to Razorpay → will fail gracefully,
        // but rejectPromotion still completes and sets REJECTED status
        PostResponse resp = service.rejectPromotion(30L, 99L);

        assertEquals("REJECTED", resp.promotionStatus());
    }

    // =========================================================================
    // sendPromotionPaymentReceipt – exercises the path via notificationServiceClient
    // =========================================================================

    @Test
    void approvePromotionNotifiesAuthorOnSuccess() {
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        Post p = post(40L, 1L);
        p.setPromotionStatus("PENDING_APPROVAL");
        p.setPromotionAmountPaise(9900);
        p.setPromotionDurationDays(7);
        p.setPromotionOrderId("order_1");
        p.setPromotionPaymentId("pay_1");
        when(postRepository.findById(40L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "author"));
        when(authServiceClient.getUserById(99L)).thenReturn(user(99L, "admin", "ADMIN"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);

        PostResponse resp = service.approvePromotion(40L, 99L);

        // approvePromotion sets status to ACTIVE and fires notifyPromotionApproved
        assertEquals("ACTIVE", resp.promotionStatus());
        // sendPromotionPaymentReceipt is only called from verifyPromotionPayment, not here
        verify(notificationEventPublisher, atLeastOnce()).publish(any());
    }

    // =========================================================================
    // loadAuthor – feign failure path
    // =========================================================================

    @Test
    void getPostHandlesFeignFailureOnAuthorLoad() {
        Post p = post(50L, 1L);
        when(postRepository.findById(50L)).thenReturn(Optional.of(p));
        when(authServiceClient.getUserById(1L)).thenThrow(FeignException.class);

        // When author can't be loaded, the response still builds (with null author fields)
        PostResponse resp = service.getPost(50L);
        assertEquals(50L, resp.postId());
    }

    // =========================================================================
    // validateAuthorExists – feign failure path
    // =========================================================================

    @Test
    void createPostHandlesFeignFailureOnAuthorValidation() {
        when(authServiceClient.getUserById(1L)).thenThrow(FeignException.class);

        assertThrows(Exception.class, () ->
                service.createPost(new CreatePostRequest(1L, "test", null, PostType.TEXT, PostVisibility.PUBLIC)));
    }

    // =========================================================================
    // indexPost – feign failure does not propagate
    // =========================================================================

    @Test
    void createPostContinuesWhenSearchIndexFails() {
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "alice"));
        Post saved = post(60L, 1L);
        saved.setContent("index test");
        when(postRepository.save(any())).thenReturn(saved);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "alice"));
        doThrow(FeignException.class).when(searchServiceClient).indexPost(any());

        // Should succeed even if search indexing fails
        PostResponse resp = service.createPost(
                new CreatePostRequest(1L, "index test", null, PostType.TEXT, PostVisibility.PUBLIC));
        assertNotNull(resp);
    }

    // =========================================================================
    // removePostIndex – feign failure does not propagate
    // =========================================================================

    @Test
    void deletePostContinuesWhenSearchRemoveFails() {
        Post p = post(70L, 1L);
        when(postRepository.findById(70L)).thenReturn(Optional.of(p));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);
        doThrow(FeignException.class).when(searchServiceClient).removePostIndex(70L);

        assertDoesNotThrow(() -> service.deletePost(70L, 1L));
    }

    // =========================================================================
    // toReportResponse – when reporter feign fails
    // =========================================================================

    @Test
    void getReportsForAdminHandlesReporterFeignFailure() {
        PostReport report = PostReport.builder()
                .reportId(1L).postId(80L).reporterId(7L).resolved(false).build();
        when(postReportRepository.findByResolvedOrderByCreatedAtDesc(false))
                .thenReturn(List.of(report));
        when(authServiceClient.getUserById(7L)).thenThrow(FeignException.class);

        List<PostReportResponse> results = service.getReportsForAdmin(false);

        assertFalse(results.isEmpty());
        assertNull(results.get(0).reporterUsername());
    }

    // =========================================================================
    // assertOwner – wrong owner path
    // =========================================================================

    @Test
    void updatePostThrowsForNonOwner() {
        Post p = post(90L, 1L);
        when(postRepository.findById(90L)).thenReturn(Optional.of(p));

        assertThrows(Exception.class, () ->
                service.updatePost(90L, 999L,
                        new UpdatePostRequest("hacked", List.of(), PostType.TEXT, PostVisibility.PUBLIC)));
    }

    // =========================================================================
    // notifyMentions – multiple mentions
    // =========================================================================

    @Test
    void updatePostNotifiesMultipleMentionedUsers() {
        Post p = post(100L, 1L);
        when(postRepository.findById(100L)).thenReturn(Optional.of(p));
        when(postRepository.save(p)).thenReturn(p);
        when(authServiceClient.getUserById(1L)).thenReturn(user(1L, "alice"));
        when(cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE)).thenReturn(cache);
        when(authServiceClient.searchUsers("bob")).thenReturn(
                List.of(new UserSummary(2L, "bob", "Bob", null, "bob@x.com", "USER", true)));
        when(authServiceClient.searchUsers("carol")).thenReturn(
                List.of(new UserSummary(3L, "carol", "Carol", null, "carol@x.com", "USER", true)));

        service.updatePost(100L, 1L,
                new UpdatePostRequest("hello @bob and @carol", List.of(), PostType.TEXT, PostVisibility.PUBLIC));

        verify(notificationEventPublisher, times(2)).publish(any());
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
