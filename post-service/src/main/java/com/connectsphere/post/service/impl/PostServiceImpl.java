package com.connectsphere.post.service.impl;

import com.connectsphere.post.client.AuthServiceClient;
import com.connectsphere.post.client.CreateNotificationRequest;
import com.connectsphere.post.client.FollowServiceClient;
import com.connectsphere.post.client.IndexPostRequest;
import com.connectsphere.post.client.NotificationServiceClient;
import com.connectsphere.post.client.NotificationType;
import com.connectsphere.post.client.PaymentReceiptEmailRequest;
import com.connectsphere.post.client.SearchServiceClient;
import com.connectsphere.post.client.UserSummary;
import com.connectsphere.post.config.CacheConfig;
import com.connectsphere.post.dto.AdminModeratePostRequest;
import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.CreatePromotionOrderRequest;
import com.connectsphere.post.dto.CreatePromotionOrderResponse;
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
import com.connectsphere.post.exception.ResourceNotFoundException;
import com.connectsphere.post.messaging.NotificationEventPublisher;
import com.connectsphere.post.repository.BookmarkRepository;
import com.connectsphere.post.repository.PostReportRepository;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.service.MediaStorageService;
import com.connectsphere.post.service.PostService;
import com.connectsphere.post.util.PostMapper;
import feign.FeignException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Implements Post business operations.
 */


@Service
@RequiredArgsConstructor
@Transactional

public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<!\\w)@([a-zA-Z0-9._-]{3,40})");
    private static final List<String> FLAGGED_KEYWORDS = List.of("spam", "scam", "hate", "abuse", "nsfw");

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final PostReportRepository postReportRepository;
    private final AuthServiceClient authServiceClient;
    private final FollowServiceClient followServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final SearchServiceClient searchServiceClient;
    private final NotificationEventPublisher notificationEventPublisher;
    private final MediaStorageService mediaStorageService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.promote-post-amount-paise:9900}")
    private int defaultPromotionAmountPaise;

    @Value("${razorpay.promote-post-duration-days:7}")
    private int defaultPromotionDurationDays;
/**
 * Creates post.
 * @param request request payload
 * @return operation result
 */

    @Override
    public PostResponse createPost(CreatePostRequest request) {
        validateAuthorExists(request.authorId());

        Post post = Post.builder()
                .authorId(request.authorId())
                .content(request.content().trim())
                .mediaUrls(request.safeMediaUrls())
                .postType(resolvePostType(request.postType(), request.safeMediaUrls()))
                .visibility(request.visibility() == null ? PostVisibility.PUBLIC : request.visibility())
                .moderationStatus(resolveModerationStatus(request.content()))
                .moderationReason(resolveModerationReason(request.content()))
                .automatedFlagged(isAutomatedFlagged(request.content()))
                .build();

        Post savedPost = postRepository.save(post);
        indexPost(savedPost);
        notifyMentions(savedPost, post.getContent(), Set.of());
        return toResponse(savedPost);
    }
/**
 * Returns post.
 * @param postId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.POST_DETAILS_CACHE, key = "#root.args[0]")
    public PostResponse getPost(Long postId) {
        Post post = findActivePost(postId);
        UserSummary author = loadAuthor(post);
        return PostMapper.toResponse(post, author);
    }
/**
 * Returns public posts.
 * @param viewerId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getPublicPosts(Long viewerId, Pageable pageable) {
        if (viewerId != null && viewerId > 0) {
            return mapVisiblePosts(
                    postRepository.findPublicFeedExcludingAuthor(PostVisibility.PUBLIC, viewerId, pageable),
                    pageable
            );
        }

        return mapVisiblePosts(
                postRepository.findPublicFeedWithPromotedFirst(PostVisibility.PUBLIC, pageable),
                pageable
        );
    }
/**
 * Returns user posts.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable) {
        return mapVisiblePosts(
                postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable),
                pageable
        );
    }
/**
 * Returns feed.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Long userId, Pageable pageable) {
        if (userId == null || userId <= 0) {
            return getPublicPosts(null, pageable);
        }

        List<Long> followingIds = loadFollowingIds(userId);
        if (followingIds.isEmpty()) {
            return mapVisiblePosts(postRepository.findFeedPostsWithoutFollowees(userId, pageable), pageable);
        }

        return mapVisiblePosts(
                postRepository.findFeedPostsWithFollowees(userId, followingIds, pageable),
                pageable);
    }
/**
 * Searches public posts.
 * @param keyword search term
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> searchPublicPosts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return getPublicPosts(null, pageable);
        }
        return mapVisiblePosts(postRepository.searchPublicPosts(keyword.trim(), pageable), pageable);
    }
/**
 * Returns posts by visibility.
 * @param visibility method input parameter
 * @param pageable pagination information
 * @return requested page of results
 */

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByVisibility(PostVisibility visibility, Pageable pageable) {
        return mapVisiblePosts(
                postRepository.findByVisibilityAndDeletedFalseOrderByCreatedAtDesc(visibility, pageable),
                pageable
        );
    }
/**
 * Updates post.
 * @param postId entity identifier
 * @param requesterId entity identifier
 * @param request request payload
 * @return operation result
 */

    @Override
    public PostResponse updatePost(Long postId, Long requesterId, UpdatePostRequest request) {
        Post post = findActivePost(postId);
        assertOwner(post, requesterId);
        Set<String> previousMentions = extractMentionUsernames(post.getContent());
        post.setContent(request.content().trim());
        post.setMediaUrls(request.safeMediaUrls());
        post.setPostType(resolvePostType(request.postType(), request.safeMediaUrls()));
        post.setVisibility(request.visibility() == null ? post.getVisibility() : request.visibility());
        post.setAutomatedFlagged(isAutomatedFlagged(post.getContent()));
        post.setModerationStatus(post.isAutomatedFlagged() ? ModerationStatus.FLAGGED : ModerationStatus.APPROVED);
        post.setModerationReason(resolveModerationReason(post.getContent()));
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        removePostIndex(savedPost.getPostId());
        indexPost(savedPost);
        notifyMentions(savedPost, savedPost.getContent(), previousMentions);
        return toResponse(savedPost);
    }
/**
 * Deletes post.
 * @param postId entity identifier
 * @param requesterId entity identifier
 */

    @Override
    public void deletePost(Long postId, Long requesterId) {
        Post post = findActivePost(postId);
        assertOwner(post, requesterId);
        post.setDeleted(true);
        postRepository.save(post);
        evictPostCache(postId);
        bookmarkRepository.deleteByPostId(postId);
        removePostIndex(post.getPostId());
    }
/**
 * Updates counter.
 * @param postId entity identifier
 * @param request request payload
 * @return operation result
 */

    @Override
    public PostResponse updateCounter(Long postId, PostCounterRequest request) {
        Post post = findActivePost(postId);
        long delta = request.delta();
        switch (request.counterType().toLowerCase()) {
            case "likes" -> post.setLikesCount(nonNegative(post.getLikesCount() + delta));
            case "comments" -> post.setCommentsCount(nonNegative(post.getCommentsCount() + delta));
            case "shares" -> post.setSharesCount(nonNegative(post.getSharesCount() + delta));
            default -> throw new BadRequestException("counterType must be likes, comments, or shares");
        }
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        return toResponse(savedPost);
    }
/**
 * Shares post.
 * @param postId entity identifier
 * @param requesterId entity identifier
 * @return resulting value
 */

    @Override
    public PostResponse sharePost(Long postId, Long requesterId) {
        if (requesterId == null || requesterId <= 0) {
            throw new BadRequestException("requesterId is required");
        }

        Post post = findActivePost(postId);
        post.setSharesCount(nonNegative(post.getSharesCount() + 1));
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        return toResponse(savedPost);
    }
/**
 * Creates promotion order.
 * @param postId entity identifier
 * @param request request payload
 * @return operation result
 */

    @Override
    public CreatePromotionOrderResponse createPromotionOrder(Long postId, CreatePromotionOrderRequest request) {
        assertRazorpayConfigured();
        Post post = findActivePost(postId);
        assertOwner(post, request.userId());

        int amountPaise = request.amountPaise() == null ? defaultPromotionAmountPaise : request.amountPaise();
        int durationDays = request.durationDays() == null ? defaultPromotionDurationDays : request.durationDays();
        if (amountPaise < 100) {
            throw new BadRequestException("Promotion amount must be at least 100 paise");
        }
        if (durationDays < 1) {
            throw new BadRequestException("Promotion duration must be at least 1 day");
        }

        String orderId = createRazorpayOrder(amountPaise, postId);
        post.setPromotionOrderId(orderId);
        post.setPromotionPaymentId(null);
        post.setPromotionAmountPaise(amountPaise);
        post.setPromotionDurationDays(durationDays);
        post.setPromotionStatus("PENDING");
        postRepository.save(post);
        evictPostCache(post.getPostId());

        return new CreatePromotionOrderResponse(
                razorpayKeyId,
                orderId,
                amountPaise,
                "INR",
                postId,
                durationDays
        );
    }
/**
 * Verifies promotion payment.
 * @param postId entity identifier
 * @param request request payload
 * @return operation result
 */

    @Override
    public PostResponse verifyPromotionPayment(Long postId, VerifyPromotionPaymentRequest request) {
        assertRazorpayConfigured();
        Post post = findActivePost(postId);
        assertOwner(post, request.userId());

        if (!request.razorpayOrderId().equals(post.getPromotionOrderId())) {
            throw new BadRequestException("Payment order does not match this post");
        }

        if (!isValidRazorpaySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            post.setPromotionStatus("FAILED");
            postRepository.save(post);
            evictPostCache(post.getPostId());
            throw new BadRequestException("Payment verification failed");
        }

        post.setPromoted(false);
        post.setPromotedUntil(null);
        post.setPromotionPaymentId(request.razorpayPaymentId());
        post.setPromotionStatus("PENDING_APPROVAL");
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        sendPromotionPaymentReceipt(savedPost);
        return toResponse(savedPost);
    }
/**
 * Adds bookmark.
 * @param postId entity identifier
 * @param userId entity identifier
 */

    @Override
    public void addBookmark(Long postId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId is required");
        }

        Post post = findActivePost(postId);
        if (post.getVisibility() == PostVisibility.PRIVATE && !post.getAuthorId().equals(userId)) {
            throw new ForbiddenException("You cannot bookmark this post");
        }

        if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
            return;
        }

        bookmarkRepository.save(Bookmark.builder()
                .userId(userId)
                .postId(postId)
                .build());
    }
/**
 * Removes bookmark.
 * @param postId entity identifier
 * @param userId entity identifier
 */

    @Override
    public void removeBookmark(Long postId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId is required");
        }

        bookmarkRepository.findByUserIdAndPostId(userId, postId)
                .ifPresent(bookmarkRepository::delete);
    }
/**
 * Determines whether bookmarked.
 * @param postId entity identifier
 * @param userId entity identifier
 * @return true when the condition is satisfied; otherwise false
 */

    @Override
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long postId, Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }

        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }
/**
 * Returns bookmarked posts.
 * @param userId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getBookmarkedPosts(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId is required");
        }

        List<Long> bookmarkedPostIds = bookmarkRepository.findPostIdsByUserIdOrderByCreatedAtDesc(userId);
        if (bookmarkedPostIds.isEmpty()) {
            return List.of();
        }

        List<Post> bookmarkedPosts = postRepository.findByPostIdInAndDeletedFalse(bookmarkedPostIds);
        if (bookmarkedPosts.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> sortOrder = new HashMap<>();
        for (int index = 0; index < bookmarkedPostIds.size(); index++) {
            sortOrder.put(bookmarkedPostIds.get(index), index);
        }

        return bookmarkedPosts.stream()
                .sorted(Comparator.comparingInt(post -> sortOrder.getOrDefault(post.getPostId(), Integer.MAX_VALUE)))
                .map(this::toResponse)
                .toList();
    }
/**
 * Counts active posts.
 * @return resulting value
 */

    @Override
    @Transactional(readOnly = true)
    public long countActivePosts() {
        return postRepository.countByDeletedFalse();
    }
/**
 * Deletes posts by author.
 * @param authorId entity identifier
 */

    @Override
    public void deletePostsByAuthor(Long authorId) {
        if (authorId == null) {
            throw new BadRequestException("authorId is required");
        }

        bookmarkRepository.deleteByUserId(authorId);

        List<Post> posts = postRepository.findByAuthorId(authorId);
        List<Long> postIds = posts.stream().map(Post::getPostId).toList();
        if (!postIds.isEmpty()) {
            bookmarkRepository.deleteByPostIdIn(postIds);
        }
        for (Post post : posts) {
            for (String mediaUrl : post.getMediaUrls()) {
                mediaStorageService.deletePostMediaByUrl(mediaUrl);
            }
            evictPostCache(post.getPostId());
            removePostIndex(post.getPostId());
        }
        postRepository.deleteAll(posts);
    }
/**
 * Returns all posts for admin.
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPostsForAdmin() {
        return postRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Returns flagged posts for admin.
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getFlaggedPostsForAdmin() {
        return postRepository.findByAutomatedFlaggedTrueAndDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Returns pending promotions for admin.
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getPendingPromotionsForAdmin() {
        return postRepository.findByPromotionStatusAndDeletedFalseOrderByUpdatedAtDesc("PENDING_APPROVAL").stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Performs the admin update post operation.
 * @param postId entity identifier
 * @param adminUserId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @Override
    public PostResponse adminUpdatePost(Long postId, Long adminUserId, UpdatePostRequest request) {
        assertAdmin(adminUserId);
        Post post = findActivePost(postId);
        Set<String> previousMentions = extractMentionUsernames(post.getContent());
        post.setContent(request.content().trim());
        post.setMediaUrls(request.safeMediaUrls());
        post.setPostType(resolvePostType(request.postType(), request.safeMediaUrls()));
        post.setVisibility(request.visibility() == null ? post.getVisibility() : request.visibility());
        post.setAutomatedFlagged(isAutomatedFlagged(post.getContent()));
        post.setModerationStatus(post.isAutomatedFlagged() ? ModerationStatus.FLAGGED : ModerationStatus.APPROVED);
        post.setModerationReason(resolveModerationReason(post.getContent()));
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        removePostIndex(savedPost.getPostId());
        indexPost(savedPost);
        notifyMentions(savedPost, savedPost.getContent(), previousMentions);
        return toResponse(savedPost);
    }
/**
 * Performs the admin delete post operation.
 * @param postId entity identifier
 * @param adminUserId entity identifier
 */

    @Override
    public void adminDeletePost(Long postId, Long adminUserId) {
        assertAdmin(adminUserId);
        Post post = findActivePost(postId);
        post.setDeleted(true);
        post.setModerationStatus(ModerationStatus.REMOVED);
        postReportRepository.findByResolvedOrderByCreatedAtDesc(false).stream()
                .filter(report -> postId.equals(report.getPostId()))
                .forEach(report -> {
                    report.setResolved(true);
                    report.setResolvedBy(adminUserId);
                    report.setResolutionNote("Post removed by admin");
                });
        postRepository.save(post);
        evictPostCache(postId);
        bookmarkRepository.deleteByPostId(postId);
        removePostIndex(post.getPostId());
    }
/**
 * Performs the approve promotion operation.
 * @param postId entity identifier
 * @param adminUserId entity identifier
 * @return resulting value
 */

    @Override
    public PostResponse approvePromotion(Long postId, Long adminUserId) {
        assertAdmin(adminUserId);
        Post post = findActivePost(postId);
        if (!isPromotionPendingReview(post)) {
            throw new BadRequestException("Promotion is not pending approval");
        }

        int durationDays = post.getPromotionDurationDays() == null ? defaultPromotionDurationDays : post.getPromotionDurationDays();
        post.setPromoted(true);
        post.setPromotedUntil(Instant.now().plusSeconds((long) durationDays * 24 * 60 * 60));
        post.setPromotionStatus("ACTIVE");
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        notifyPromotionApproved(savedPost, adminUserId);
        return toResponse(savedPost);
    }
/**
 * Performs the reject promotion operation.
 * @param postId entity identifier
 * @param adminUserId entity identifier
 * @return resulting value
 */

    @Override
    public PostResponse rejectPromotion(Long postId, Long adminUserId) {
        assertAdmin(adminUserId);
        Post post = findActivePost(postId);
        if (!isPromotionPendingReview(post)) {
            throw new BadRequestException("Promotion is not pending approval");
        }

        boolean refundInitiated = false;
        if (post.getPromotionPaymentId() != null && !post.getPromotionPaymentId().isBlank()) {
            refundInitiated = refundPromotionPayment(post);
        }

        post.setPromoted(false);
        post.setPromotedUntil(null);
        post.setPromotionStatus(refundInitiated ? "REJECTED_REFUNDING" : "REJECTED");
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        notifyPromotionRejected(savedPost, adminUserId, refundInitiated);
        return toResponse(savedPost);
    }

    private boolean isPromotionPendingReview(Post post) {
        return "PENDING".equals(post.getPromotionStatus()) || "PENDING_APPROVAL".equals(post.getPromotionStatus());
    }
/**
 * Performs the moderate post operation.
 * @param postId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @Override
    public PostResponse moderatePost(Long postId, AdminModeratePostRequest request) {
        assertAdmin(request.adminUserId());
        Post post = findActivePost(postId);
        post.setModerationStatus(request.moderationStatus());
        post.setModerationReason(request.moderationReason());
        if (request.moderationStatus() == ModerationStatus.REMOVED) {
            post.setDeleted(true);
            bookmarkRepository.deleteByPostId(postId);
            removePostIndex(postId);
        } else if (request.moderationStatus() == ModerationStatus.APPROVED) {
            post.setAutomatedFlagged(false);
        }
        Post savedPost = postRepository.save(post);
        evictPostCache(savedPost.getPostId());
        return toResponse(savedPost);
    }
/**
 * Reports post.
 * @param postId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @Override
    public PostReportResponse reportPost(Long postId, PostReportRequest request) {
        findActivePost(postId);
        validateAuthorExists(request.reporterId());
        PostReport report = postReportRepository.save(PostReport.builder()
                .postId(postId)
                .reporterId(request.reporterId())
                .reason(request.reason().trim())
                .resolved(false)
                .build());
        return toReportResponse(report);
    }
/**
 * Returns reports for admin.
 * @param resolved method input parameter
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<PostReportResponse> getReportsForAdmin(boolean resolved) {
        return postReportRepository.findByResolvedOrderByCreatedAtDesc(resolved).stream()
                .map(this::toReportResponse)
                .toList();
    }
/**
 * Resolves report.
 * @param reportId entity identifier
 * @param request request payload
 * @return operation result
 */

    @Override
    public PostReportResponse resolveReport(Long reportId, ResolvePostReportRequest request) {
        assertAdmin(request.adminUserId());
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setResolved(true);
        report.setResolvedBy(request.adminUserId());
        report.setResolutionNote(request.resolutionNote());

        if (request.removePost()) {
            Post post = findActivePost(report.getPostId());
            post.setDeleted(true);
            post.setModerationStatus(ModerationStatus.REMOVED);
            post.setModerationReason(request.resolutionNote());
            postRepository.save(post);
            evictPostCache(post.getPostId());
            bookmarkRepository.deleteByPostId(post.getPostId());
            removePostIndex(post.getPostId());
        }

        return toReportResponse(postReportRepository.save(report));
    }

    private Post findActivePost(Long postId) {
        return postRepository.findById(postId)
                .filter(post -> !post.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private void assertOwner(Post post, Long requesterId) {
        if (requesterId == null) {
            throw new BadRequestException("requesterId is required");
        }
        if (!post.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("You can only modify your own posts");
        }
    }

    private void assertAdmin(Long requesterId) {
        if (requesterId == null || requesterId <= 0) {
            throw new BadRequestException("adminUserId is required");
        }
        try {
            UserSummary user = authServiceClient.getUserById(requesterId);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.role())) {
                throw new ForbiddenException("Admin access required");
            }
        } catch (FeignException ex) {
            throw new ForbiddenException("Could not verify admin access");
        }
    }

    private PostType resolvePostType(PostType requestedType, List<String> mediaUrls) {
        if (requestedType != null) {
            return requestedType;
        }
        return mediaUrls == null || mediaUrls.isEmpty() ? PostType.TEXT : PostType.IMAGE;
    }

    private long nonNegative(long value) {
        return Math.max(0, value);
    }

    private void assertRazorpayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
    }

    private String createRazorpayOrder(int amountPaise, Long postId) {
        try {
            Map<String, Object> payload = Map.of(
                    "amount", amountPaise,
                    "currency", "INR",
                    "receipt", "post_" + postId + "_" + Instant.now().toEpochMilli()
            );
            String credentials = Base64.getEncoder()
                    .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Razorpay order creation failed: status={}, body={}", response.statusCode(), response.body());
                throw new BadRequestException("Could not create Razorpay order");
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
            Object id = body.get("id");
            if (id == null || id.toString().isBlank()) {
                throw new BadRequestException("Razorpay order response did not include order id");
            }
            return id.toString();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not create Razorpay order for postId={}", postId, ex);
            throw new BadRequestException("Could not create Razorpay order");
        }
    }

    private boolean refundPromotionPayment(Post post) {
        assertRazorpayConfigured();

        Integer refundAmount = post.getPromotionAmountPaise();
        if (refundAmount == null || refundAmount < 1) {
            log.warn("Skipping refund for postId={} because promotion amount is missing", post.getPostId());
            return false;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", refundAmount);
            payload.put("speed", "normal");
            payload.put("notes", Map.of(
                    "postId", String.valueOf(post.getPostId()),
                    "reason", "promotion_rejected"
            ));

            String credentials = Base64.getEncoder()
                    .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/payments/" + post.getPromotionPaymentId() + "/refund"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "Razorpay refund creation failed for postId={}: status={}, body={}",
                        post.getPostId(),
                        response.statusCode(),
                        response.body()
                );
                return false;
            }

            log.info("Refund initiated for postId={} paymentId={}", post.getPostId(), post.getPromotionPaymentId());
            return true;
        } catch (Exception ex) {
            log.warn("Could not initiate refund for postId={}", post.getPostId(), ex);
            return false;
        }
    }

    private boolean isValidRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(digest);
            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.warn("Could not verify Razorpay signature", ex);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }

    private boolean isAutomatedFlagged(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toLowerCase();
        return FLAGGED_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private ModerationStatus resolveModerationStatus(String content) {
        return isAutomatedFlagged(content) ? ModerationStatus.FLAGGED : ModerationStatus.APPROVED;
    }

    private String resolveModerationReason(String content) {
        if (!isAutomatedFlagged(content)) {
            return null;
        }
        String normalized = content == null ? "" : content.toLowerCase();
        return FLAGGED_KEYWORDS.stream()
                .filter(normalized::contains)
                .findFirst()
                .map(keyword -> "Automatically flagged for keyword: " + keyword)
                .orElse("Automatically flagged by moderation rules");
    }

    private PostResponse toResponse(Post post) {
        return PostMapper.toResponse(post, loadAuthor(post));
    }

    private Page<PostResponse> mapVisiblePosts(Page<Post> posts, Pageable pageable) {
        List<PostResponse> visiblePosts = posts.getContent().stream()
                .map(this::toResponseIfAuthorExists)
                .filter(response -> response != null)
                .toList();
        return new PageImpl<>(visiblePosts, pageable, visiblePosts.size());
    }

    private PostReportResponse toReportResponse(PostReport report) {
        UserSummary reporter = null;
        try {
            reporter = authServiceClient.getUserById(report.getReporterId());
        } catch (FeignException ex) {
            log.warn("Could not load reporter data for reportId={}: status={}, message={}", report.getReportId(), ex.status(), ex.getMessage());
        }
        return new PostReportResponse(
                report.getReportId(),
                report.getPostId(),
                report.getReporterId(),
                reporter == null ? null : reporter.username(),
                report.getReason(),
                report.isResolved(),
                report.getResolutionNote(),
                report.getResolvedBy(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private PostResponse toResponseIfAuthorExists(Post post) {
        UserSummary author = loadAuthor(post);
        if (author == null) {
            log.warn("Rendering postId={} without author data because authorId={} could not be loaded",
                    post.getPostId(),
                    post.getAuthorId());
        }
        return PostMapper.toResponse(post, author);
    }

    private void indexPost(Post post) {
        UserSummary author = loadAuthor(post);
        try {
            searchServiceClient.indexPost(new IndexPostRequest(
                    post.getPostId(),
                    post.getAuthorId(),
                    post.getContent(),
                    author == null ? null : author.username(),
                    post.getVisibility().name()
            ));
        } catch (FeignException ex) {
            log.warn(
                    "Could not index postId={} in search-service: status={}, message={}",
                    post.getPostId(),
                    ex.status(),
                    ex.getMessage()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not index postId={} in search-service", post.getPostId(), ex);
        }
    }

    private void removePostIndex(Long postId) {
        try {
            searchServiceClient.removePostIndex(postId);
        } catch (FeignException ex) {
            log.warn(
                    "Could not remove postId={} from search-service: status={}, message={}",
                    postId,
                    ex.status(),
                    ex.getMessage()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not remove postId={} from search-service", postId, ex);
        }
    }

    private void evictPostCache(Long postId) {
        Cache postDetails = cacheManager.getCache(CacheConfig.POST_DETAILS_CACHE);
        if (postDetails != null) {
            postDetails.evictIfPresent(postId);
        }
    }

    private UserSummary loadAuthor(Post post) {
        try {
            return authServiceClient.getUserById(post.getAuthorId());
        } catch (FeignException ex) {
            log.warn(
                    "Could not load public author data for userId={} while rendering postId={}: status={}, message={}",
                    post.getAuthorId(),
                    post.getPostId(),
                    ex.status(),
                    ex.getMessage()
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "Could not load public author data for userId={} while rendering postId={}",
                    post.getAuthorId(),
                    post.getPostId(),
                    ex
            );
        }
        return null;
    }

    private void validateAuthorExists(Long authorId) {
        if (authorId == null || authorId <= 0) {
            throw new BadRequestException("authorId is required");
        }

        try {
            authServiceClient.getUserById(authorId);
        } catch (FeignException ex) {
            throw new BadRequestException("authorId does not exist");
        } catch (RuntimeException ex) {
            throw new BadRequestException("Could not verify post author");
        }
    }

    private List<Long> loadFollowingIds(Long userId) {
        try {
            List<Long> followingIds = followServiceClient.getFollowingIds(userId);
            return followingIds == null ? List.of() : followingIds;
        } catch (FeignException ex) {
            log.warn("Could not load following IDs for userId={}: status={}, message={}",
                    userId,
                    ex.status(),
                    ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Could not load following IDs for userId={}", userId, ex);
        }
        return List.of();
    }

    private void notifyMentions(Post post, String content, Set<String> previousMentions) {
        Set<String> mentionUsernames = extractMentionUsernames(content);
        if (mentionUsernames.isEmpty()) {
            return;
        }

        mentionUsernames.removeAll(previousMentions);
        if (mentionUsernames.isEmpty()) {
            return;
        }

        UserSummary author = loadAuthor(post);
        String actorName = author != null && author.fullName() != null && !author.fullName().isBlank()
                ? author.fullName()
                : author != null && author.username() != null && !author.username().isBlank()
                ? "@" + author.username()
                : "Someone";

        Set<Long> notifiedUserIds = new HashSet<>();
        for (String username : mentionUsernames) {
            try {
                List<UserSummary> results = authServiceClient.searchUsers(username);
                UserSummary mentionedUser = results.stream()
                        .filter(user -> user.username() != null && user.username().equalsIgnoreCase(username))
                        .findFirst()
                        .orElse(null);

                if (mentionedUser == null || mentionedUser.userId() == null) {
                    continue;
                }

                if (mentionedUser.userId().equals(post.getAuthorId()) || !notifiedUserIds.add(mentionedUser.userId())) {
                    continue;
                }

                notificationEventPublisher.publish(new CreateNotificationRequest(
                        mentionedUser.userId(),
                        post.getAuthorId(),
                        NotificationType.MENTION,
                        actorName + " mentioned you in a post",
                        post.getPostId(),
                        "POST"
                ));
            } catch (FeignException ex) {
                log.warn(
                        "Could not notify mention @{} for postId={}: status={}, message={}",
                        username,
                        post.getPostId(),
                        ex.status(),
                        ex.getMessage()
                );
            } catch (RuntimeException ex) {
                log.warn("Could not notify mention @{} for postId={}", username, post.getPostId(), ex);
            }
        }
    }

    private void notifyPromotionApproved(Post post, Long adminUserId) {
        try {
            UserSummary admin = authServiceClient.getUserById(adminUserId);
            String adminName = admin != null && admin.fullName() != null && !admin.fullName().isBlank()
                    ? admin.fullName()
                    : admin != null && admin.username() != null && !admin.username().isBlank()
                    ? "@" + admin.username()
                    : "An admin";

            notificationEventPublisher.publish(new CreateNotificationRequest(
                    post.getAuthorId(),
                    adminUserId,
                    NotificationType.SYSTEM,
                    "Your promoted post was approved by " + adminName + " and is now active.",
                    post.getPostId(),
                    "POST"
            ));
        } catch (FeignException ex) {
            log.warn(
                    "Could not load admin details to notify promotion approval for postId={}: status={}, message={}",
                    post.getPostId(),
                    ex.status(),
                    ex.getMessage()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not notify promotion approval for postId={}", post.getPostId(), ex);
        }
    }

    private void notifyPromotionRejected(Post post, Long adminUserId, boolean refundInitiated) {
        try {
            UserSummary admin = authServiceClient.getUserById(adminUserId);
            String adminName = admin != null && admin.fullName() != null && !admin.fullName().isBlank()
                    ? admin.fullName()
                    : admin != null && admin.username() != null && !admin.username().isBlank()
                    ? "@" + admin.username()
                    : "An admin";

            String message = refundInitiated
                    ? "Your promoted post was rejected by " + adminName + ". A refund has been initiated to your original payment method."
                    : "Your promoted post was rejected by " + adminName + ". If your payment was captured, your refund may require manual review.";

            notificationEventPublisher.publish(new CreateNotificationRequest(
                    post.getAuthorId(),
                    adminUserId,
                    NotificationType.SYSTEM,
                    message,
                    post.getPostId(),
                    "POST"
            ));
        } catch (FeignException ex) {
            log.warn(
                    "Could not load admin details to notify promotion rejection for postId={}: status={}, message={}",
                    post.getPostId(),
                    ex.status(),
                    ex.getMessage()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not notify promotion rejection for postId={}", post.getPostId(), ex);
        }
    }

    private void sendPromotionPaymentReceipt(Post post) {
        UserSummary author = loadAuthor(post);
        if (author == null || author.email() == null || author.email().isBlank()) {
            log.warn("Skipping payment receipt email for postId={} because author email is unavailable", post.getPostId());
            return;
        }

        try {
            notificationServiceClient.sendPaymentReceiptEmail(new PaymentReceiptEmailRequest(
                    author.userId(),
                    author.email(),
                    author.username() == null ? "user" : author.username(),
                    author.fullName() == null || author.fullName().isBlank() ? author.username() : author.fullName(),
                    post.getPostId(),
                    post.getPromotionOrderId(),
                    post.getPromotionPaymentId(),
                    post.getPromotionAmountPaise() == null ? defaultPromotionAmountPaise : post.getPromotionAmountPaise(),
                    post.getPromotionDurationDays() == null ? defaultPromotionDurationDays : post.getPromotionDurationDays()
            ));
        } catch (FeignException ex) {
            log.warn(
                    "Could not send payment receipt email for postId={}: status={}, message={}",
                    post.getPostId(),
                    ex.status(),
                    ex.getMessage()
            );
        } catch (RuntimeException ex) {
            log.warn("Could not send payment receipt email for postId={}", post.getPostId(), ex);
        }
    }

    private Set<String> extractMentionUsernames(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }

        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1).trim().toLowerCase());
        }
        return usernames;
    }
}
