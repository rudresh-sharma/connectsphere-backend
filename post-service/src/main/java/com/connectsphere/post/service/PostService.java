package com.connectsphere.post.service;

import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.AdminModeratePostRequest;
import com.connectsphere.post.dto.CreatePromotionOrderRequest;
import com.connectsphere.post.dto.CreatePromotionOrderResponse;
import com.connectsphere.post.dto.PostCounterRequest;
import com.connectsphere.post.dto.PostReportRequest;
import com.connectsphere.post.dto.PostReportResponse;
import com.connectsphere.post.dto.ResolvePostReportRequest;
import com.connectsphere.post.dto.PostResponse;
import com.connectsphere.post.dto.UpdatePostRequest;
import com.connectsphere.post.dto.VerifyPromotionPaymentRequest;
import com.connectsphere.post.entity.PostVisibility;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines Post business operations.
 */
public interface PostService {

    PostResponse createPost(CreatePostRequest request);

    PostResponse getPost(Long postId);

    Page<PostResponse> getPublicPosts(Long viewerId, Pageable pageable);

    Page<PostResponse> getUserPosts(Long userId, Pageable pageable);

    Page<PostResponse> getFeed(Long userId, Pageable pageable);

    Page<PostResponse> searchPublicPosts(String keyword, Pageable pageable);

    Page<PostResponse> getPostsByVisibility(PostVisibility visibility, Pageable pageable);

    PostResponse updatePost(Long postId, Long requesterId, UpdatePostRequest request);

    void deletePost(Long postId, Long requesterId);

    PostResponse updateCounter(Long postId, PostCounterRequest request);
    PostResponse sharePost(Long postId, Long requesterId);
    CreatePromotionOrderResponse createPromotionOrder(Long postId, CreatePromotionOrderRequest request);
    PostResponse verifyPromotionPayment(Long postId, VerifyPromotionPaymentRequest request);
    void addBookmark(Long postId, Long userId);
    void removeBookmark(Long postId, Long userId);
    boolean isBookmarked(Long postId, Long userId);
    List<PostResponse> getBookmarkedPosts(Long userId);

    long countActivePosts();

    void deletePostsByAuthor(Long authorId);

    List<PostResponse> getAllPostsForAdmin();

    List<PostResponse> getFlaggedPostsForAdmin();

    List<PostResponse> getPendingPromotionsForAdmin();

    PostResponse adminUpdatePost(Long postId, Long adminUserId, UpdatePostRequest request);

    void adminDeletePost(Long postId, Long adminUserId);

    PostResponse approvePromotion(Long postId, Long adminUserId);

    PostResponse rejectPromotion(Long postId, Long adminUserId);

    PostResponse moderatePost(Long postId, AdminModeratePostRequest request);

    PostReportResponse reportPost(Long postId, PostReportRequest request);

    List<PostReportResponse> getReportsForAdmin(boolean resolved);

    PostReportResponse resolveReport(Long reportId, ResolvePostReportRequest request);
}
