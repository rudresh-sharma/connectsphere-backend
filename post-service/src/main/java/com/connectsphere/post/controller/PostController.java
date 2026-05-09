package com.connectsphere.post.controller;

import com.connectsphere.post.dto.AdminModeratePostRequest;
import com.connectsphere.post.dto.CreatePostRequest;
import com.connectsphere.post.dto.CreatePromotionOrderRequest;
import com.connectsphere.post.dto.CreatePromotionOrderResponse;
import com.connectsphere.post.dto.MediaUploadResponse;
import com.connectsphere.post.dto.PostCounterRequest;
import com.connectsphere.post.dto.PostReportRequest;
import com.connectsphere.post.dto.PostReportResponse;
import com.connectsphere.post.dto.PostResponse;
import com.connectsphere.post.dto.ResolvePostReportRequest;
import com.connectsphere.post.dto.UpdatePostRequest;
import com.connectsphere.post.dto.VerifyPromotionPaymentRequest;
import com.connectsphere.post.entity.PostVisibility;
import com.connectsphere.post.service.MediaStorageService;
import com.connectsphere.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
/**
 * Exposes Post API endpoints.
 */


@RestController
@RequestMapping("/posts")

public class PostController {

    private final PostService postService;
    private final MediaStorageService mediaStorageService;

    public PostController(PostService postService, MediaStorageService mediaStorageService) {
        this.postService = postService;
        this.mediaStorageService = mediaStorageService;
    }
/**
 * Uploads media.
 * @param file uploaded file
 * @return operation result
 */

    @PostMapping("/media")
    public MediaUploadResponse uploadMedia(@RequestParam("file") MultipartFile file) {
        return mediaStorageService.uploadPostMedia(file);
    }
/**
 * Creates post.
 * @param request request payload
 * @return operation result
 */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(request);
    }
/**
 * Returns public posts.
 * @param "viewerId" method input parameter
 * @param viewerId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping
    public Page<PostResponse> getPublicPosts(
            @RequestParam(name = "viewerId", required = false) Long viewerId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return postService.getPublicPosts(viewerId, pageable);
    }
/**
 * Returns post.
 * @param id entity identifier
 * @return operation result
 */

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable("id") Long id) {
        return postService.getPost(id);
    }
/**
 * Updates post.
 * @param id entity identifier
 * @param requesterId entity identifier
 * @param request request payload
 * @return operation result
 */

    @PutMapping("/{id}")
    public PostResponse updatePost(
            @PathVariable("id") Long id,
            @RequestParam("requesterId") Long requesterId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return postService.updatePost(id, requesterId, request);
    }
/**
 * Deletes post.
 * @param id entity identifier
 * @param requesterId entity identifier
 */

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable("id") Long id,
            @RequestParam("requesterId") Long requesterId
    ) {
        postService.deletePost(id, requesterId);
    }
/**
 * Deletes posts by author.
 * @param authorId entity identifier
 */

    @DeleteMapping("/author/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePostsByAuthor(@PathVariable("authorId") Long authorId) {
        postService.deletePostsByAuthor(authorId);
    }
/**
 * Returns user posts.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/user/{userId}")
    public Page<PostResponse> getUserPosts(@PathVariable("userId") Long userId,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return postService.getUserPosts(userId, pageable);
    }
/**
 * Returns feed.
 * @param userId entity identifier
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/feed/{userId}")
    public Page<PostResponse> getFeed(@PathVariable("userId") Long userId,
                                      @PageableDefault(size = 20) Pageable pageable) {
        return postService.getFeed(userId, pageable);
    }
/**
 * Searches for posts.
 * @param "keyword" method input parameter
 * @param keyword search term
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/search")
    public Page<PostResponse> searchPosts(@RequestParam(name = "keyword", required = false) String keyword,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return postService.searchPublicPosts(keyword, pageable);
    }
/**
 * Returns by visibility.
 * @param visibility method input parameter
 * @param pageable pagination information
 * @return requested page of results
 */

    @GetMapping("/visibility")
    public Page<PostResponse> getByVisibility(@RequestParam("visibility") PostVisibility visibility,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return postService.getPostsByVisibility(visibility, pageable);
    }
/**
 * Handles the posts request.
 * @return resulting value
 */

    @GetMapping("/count")
    public long countPosts() {
        return postService.countActivePosts();
    }
/**
 * Updates counter.
 * @param id entity identifier
 * @param request request payload
 * @return operation result
 */

    @PatchMapping("/{id}/count")
    public PostResponse updateCounter(@PathVariable("id") Long id, @Valid @RequestBody PostCounterRequest request) {
        return postService.updateCounter(id, request);
    }
/**
 * Handles the post request.
 * @param id entity identifier
 * @param requesterId entity identifier
 * @return resulting value
 */

    @PostMapping("/{id}/share")
    public PostResponse sharePost(@PathVariable("id") Long id, @RequestParam("requesterId") Long requesterId) {
        return postService.sharePost(id, requesterId);
    }
/**
 * Creates promotion order.
 * @param id entity identifier
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/{id}/promotion/order")
    public CreatePromotionOrderResponse createPromotionOrder(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreatePromotionOrderRequest request
    ) {
        return postService.createPromotionOrder(id, request);
    }
/**
 * Handles the promotion payment request.
 * @param id entity identifier
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/{id}/promotion/verify")
    public PostResponse verifyPromotionPayment(
            @PathVariable("id") Long id,
            @Valid @RequestBody VerifyPromotionPaymentRequest request
    ) {
        return postService.verifyPromotionPayment(id, request);
    }
/**
 * Handles the bookmark request.
 * @param id entity identifier
 * @param userId entity identifier
 */

    @PostMapping("/{id}/bookmarks")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBookmark(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        postService.addBookmark(id, userId);
    }
/**
 * Handles the bookmark request.
 * @param id entity identifier
 * @param userId entity identifier
 */

    @DeleteMapping("/{id}/bookmarks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        postService.removeBookmark(id, userId);
    }
/**
 * Handles the bookmarked request.
 * @param id entity identifier
 * @param userId entity identifier
 * @return true when the condition is satisfied; otherwise false
 */

    @GetMapping("/{id}/bookmarks/status")
    public boolean isBookmarked(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        return postService.isBookmarked(id, userId);
    }
/**
 * Returns bookmarked posts.
 * @param userId entity identifier
 * @return matching results
 */

    @GetMapping("/bookmarks/{userId}")
    public List<PostResponse> getBookmarkedPosts(@PathVariable("userId") Long userId) {
        return postService.getBookmarkedPosts(userId);
    }
/**
 * Reports post.
 * @param id entity identifier
 * @param request request payload
 * @return resulting value
 */

    @PostMapping("/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public PostReportResponse reportPost(@PathVariable("id") Long id, @Valid @RequestBody PostReportRequest request) {
        return postService.reportPost(id, request);
    }
/**
 * Returns all posts for admin.
 * @return matching results
 */

    @GetMapping("/admin/all")
    public List<PostResponse> getAllPostsForAdmin() {
        return postService.getAllPostsForAdmin();
    }
/**
 * Returns flagged posts for admin.
 * @return matching results
 */

    @GetMapping("/admin/flagged")
    public List<PostResponse> getFlaggedPostsForAdmin() {
        return postService.getFlaggedPostsForAdmin();
    }
/**
 * Returns pending promotions for admin.
 * @return matching results
 */

    @GetMapping("/admin/promotions/pending")
    public List<PostResponse> getPendingPromotionsForAdmin() {
        return postService.getPendingPromotionsForAdmin();
    }
/**
 * Handles the admin update post request.
 * @param id entity identifier
 * @param adminUserId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @PutMapping("/admin/{id}")
    public PostResponse adminUpdatePost(
            @PathVariable("id") Long id,
            @RequestParam("adminUserId") Long adminUserId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return postService.adminUpdatePost(id, adminUserId, request);
    }
/**
 * Handles the admin delete post request.
 * @param id entity identifier
 * @param adminUserId entity identifier
 */

    @DeleteMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDeletePost(@PathVariable("id") Long id, @RequestParam("adminUserId") Long adminUserId) {
        postService.adminDeletePost(id, adminUserId);
    }
/**
 * Handles the approve promotion request.
 * @param id entity identifier
 * @param adminUserId entity identifier
 * @return resulting value
 */

    @PatchMapping("/admin/{id}/promotion/approve")
    public PostResponse approvePromotion(@PathVariable("id") Long id, @RequestParam("adminUserId") Long adminUserId) {
        return postService.approvePromotion(id, adminUserId);
    }
/**
 * Handles the reject promotion request.
 * @param id entity identifier
 * @param adminUserId entity identifier
 * @return resulting value
 */

    @PatchMapping("/admin/{id}/promotion/reject")
    public PostResponse rejectPromotion(@PathVariable("id") Long id, @RequestParam("adminUserId") Long adminUserId) {
        return postService.rejectPromotion(id, adminUserId);
    }
/**
 * Handles the moderate post request.
 * @param id entity identifier
 * @param request request payload
 * @return resulting value
 */

    @PatchMapping("/admin/{id}/moderation")
    public PostResponse moderatePost(@PathVariable("id") Long id, @Valid @RequestBody AdminModeratePostRequest request) {
        return postService.moderatePost(id, request);
    }
/**
 * Returns reports for admin.
 * @param "resolved" method input parameter
 * @param resolved method input parameter
 * @return matching results
 */

    @GetMapping("/admin/reports")
    public List<PostReportResponse> getReportsForAdmin(@RequestParam(value = "resolved", defaultValue = "false") boolean resolved) {
        return postService.getReportsForAdmin(resolved);
    }
/**
 * Handles the report request.
 * @param reportId entity identifier
 * @param request request payload
 * @return operation result
 */

    @PatchMapping("/admin/reports/{reportId}/resolve")
    public PostReportResponse resolveReport(
            @PathVariable("reportId") Long reportId,
            @Valid @RequestBody ResolvePostReportRequest request
    ) {
        return postService.resolveReport(reportId, request);
    }
}
