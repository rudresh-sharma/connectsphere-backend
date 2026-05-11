package com.connectsphere.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.connectsphere.post.entity.ModerationStatus;
import com.connectsphere.post.entity.PostType;
import com.connectsphere.post.entity.PostVisibility;
import com.connectsphere.post.service.MediaStorageService;
import com.connectsphere.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller-layer tests for PostController targeting ≥80% coverage.
 */
@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PostService postService;
    @MockBean private MediaStorageService mediaStorageService;

    // ---- POST /posts --------------------------------------------------------

    @Test
    void createPostReturns201() throws Exception {
        CreatePostRequest req = new CreatePostRequest(1L, "hello world", null, null, null);
        when(postService.createPost(any())).thenReturn(pr(10L, 1L, "hello world"));

        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(10));
    }

    // ---- GET /posts ---------------------------------------------------------

    @Test
    void getPublicPostsReturnsPage() throws Exception {
        when(postService.getPublicPosts(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(pr(1L, 2L, "pub"))));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(1));
    }

    // ---- GET /posts/{id} ----------------------------------------------------

    @Test
    void getPostReturns200() throws Exception {
        when(postService.getPost(5L)).thenReturn(pr(5L, 1L, "content"));

        mockMvc.perform(get("/posts/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(5));
    }

    // ---- PUT /posts/{id} ----------------------------------------------------

    @Test
    void updatePostReturns200() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("updated", List.of(), PostType.TEXT, PostVisibility.PUBLIC);
        when(postService.updatePost(eq(5L), eq(1L), any())).thenReturn(pr(5L, 1L, "updated"));

        mockMvc.perform(put("/posts/5").param("requesterId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("updated"));
    }

    // ---- DELETE /posts/{id} -------------------------------------------------

    @Test
    void deletePostReturns204() throws Exception {
        doNothing().when(postService).deletePost(5L, 1L);

        mockMvc.perform(delete("/posts/5").param("requesterId", "1"))
                .andExpect(status().isNoContent());
    }

    // ---- DELETE /posts/author/{authorId} ------------------------------------

    @Test
    void deletePostsByAuthorReturns204() throws Exception {
        doNothing().when(postService).deletePostsByAuthor(7L);

        mockMvc.perform(delete("/posts/author/7"))
                .andExpect(status().isNoContent());
    }

    // ---- GET /posts/user/{userId} -------------------------------------------

    @Test
    void getUserPostsReturnsPage() throws Exception {
        when(postService.getUserPosts(eq(3L), any()))
                .thenReturn(new PageImpl<>(List.of(pr(2L, 3L, "user post"))));

        mockMvc.perform(get("/posts/user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(2));
    }

    // ---- GET /posts/feed/{userId} -------------------------------------------

    @Test
    void getFeedReturnsPage() throws Exception {
        when(postService.getFeed(eq(4L), any()))
                .thenReturn(new PageImpl<>(List.of(pr(3L, 4L, "feed post"))));

        mockMvc.perform(get("/posts/feed/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(3));
    }

    // ---- GET /posts/search --------------------------------------------------

    @Test
    void searchPostsReturnsPage() throws Exception {
        when(postService.searchPublicPosts(eq("spring"), any()))
                .thenReturn(new PageImpl<>(List.of(pr(4L, 1L, "spring boot"))));

        mockMvc.perform(get("/posts/search").param("keyword", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(4));
    }

    // ---- GET /posts/visibility ----------------------------------------------

    @Test
    void getByVisibilityReturnsPage() throws Exception {
        when(postService.getPostsByVisibility(eq(PostVisibility.PRIVATE), any()))
                .thenReturn(new PageImpl<>(List.of(pr(5L, 2L, "private"))));

        mockMvc.perform(get("/posts/visibility").param("visibility", "PRIVATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(5));
    }

    // ---- GET /posts/count ---------------------------------------------------

    @Test
    void countPostsReturnsLong() throws Exception {
        when(postService.countActivePosts()).thenReturn(99L);

        mockMvc.perform(get("/posts/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(99));
    }

    // ---- PATCH /posts/{id}/count --------------------------------------------

    @Test
    void updateCounterReturns200() throws Exception {
        when(postService.updateCounter(eq(6L), any())).thenReturn(pr(6L, 1L, "liked"));

        mockMvc.perform(patch("/posts/6/count").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PostCounterRequest("likes", 1))))
                .andExpect(status().isOk());
    }

    // ---- POST /posts/{id}/share ---------------------------------------------

    @Test
    void sharePostReturns200() throws Exception {
        when(postService.sharePost(7L, 1L)).thenReturn(pr(7L, 1L, "shared"));

        mockMvc.perform(post("/posts/7/share").param("requesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(7));
    }

    // ---- POST /posts/{id}/bookmarks -----------------------------------------

    @Test
    void addBookmarkReturns201() throws Exception {
        doNothing().when(postService).addBookmark(8L, 1L);

        mockMvc.perform(post("/posts/8/bookmarks").param("userId", "1"))
                .andExpect(status().isCreated());
    }

    // ---- DELETE /posts/{id}/bookmarks ---------------------------------------

    @Test
    void removeBookmarkReturns204() throws Exception {
        doNothing().when(postService).removeBookmark(8L, 1L);

        mockMvc.perform(delete("/posts/8/bookmarks").param("userId", "1"))
                .andExpect(status().isNoContent());
    }

    // ---- GET /posts/{id}/bookmarks/status -----------------------------------

    @Test
    void isBookmarkedReturnsBoolean() throws Exception {
        when(postService.isBookmarked(8L, 1L)).thenReturn(true);

        mockMvc.perform(get("/posts/8/bookmarks/status").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    // ---- GET /posts/bookmarks/{userId} --------------------------------------

    @Test
    void getBookmarkedPostsReturnsList() throws Exception {
        when(postService.getBookmarkedPosts(9L))
                .thenReturn(List.of(pr(20L, 9L, "bookmarked")));

        mockMvc.perform(get("/posts/bookmarks/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(20));
    }

    // ---- POST /posts/{id}/reports -------------------------------------------

    @Test
    void reportPostReturns201() throws Exception {
        PostReportRequest req = new PostReportRequest(5L, "spam");
        PostReportResponse resp = new PostReportResponse(1L, 10L, 5L, "reporter",
                "spam", false, null, null, Instant.now(), Instant.now());
        when(postService.reportPost(eq(10L), any())).thenReturn(resp);

        mockMvc.perform(post("/posts/10/reports").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(1));
    }

    // ---- Admin endpoints ----------------------------------------------------

    @Test
    void getAllPostsForAdminReturnsList() throws Exception {
        when(postService.getAllPostsForAdmin()).thenReturn(List.of(pr(30L, 1L, "ap")));

        mockMvc.perform(get("/posts/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(30));
    }

    @Test
    void getFlaggedPostsForAdminReturnsList() throws Exception {
        when(postService.getFlaggedPostsForAdmin()).thenReturn(List.of(pr(31L, 1L, "flagged")));

        mockMvc.perform(get("/posts/admin/flagged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(31));
    }

    @Test
    void getPendingPromotionsForAdminReturnsList() throws Exception {
        when(postService.getPendingPromotionsForAdmin()).thenReturn(List.of(pr(32L, 1L, "pp")));

        mockMvc.perform(get("/posts/admin/promotions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(32));
    }

    @Test
    void adminUpdatePostReturns200() throws Exception {
        UpdatePostRequest req = new UpdatePostRequest("ae", List.of(), PostType.TEXT, PostVisibility.PUBLIC);
        when(postService.adminUpdatePost(eq(40L), eq(99L), any())).thenReturn(pr(40L, 1L, "ae"));

        mockMvc.perform(put("/posts/admin/40").param("adminUserId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(40));
    }

    @Test
    void adminDeletePostReturns204() throws Exception {
        doNothing().when(postService).adminDeletePost(41L, 99L);

        mockMvc.perform(delete("/posts/admin/41").param("adminUserId", "99"))
                .andExpect(status().isNoContent());
    }

    @Test
    void approvePromotionReturns200() throws Exception {
        when(postService.approvePromotion(50L, 99L)).thenReturn(pr(50L, 1L, "approved"));

        mockMvc.perform(patch("/posts/admin/50/promotion/approve").param("adminUserId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(50));
    }

    @Test
    void rejectPromotionReturns200() throws Exception {
        when(postService.rejectPromotion(51L, 99L)).thenReturn(pr(51L, 1L, "rejected"));

        mockMvc.perform(patch("/posts/admin/51/promotion/reject").param("adminUserId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(51));
    }

    @Test
    void moderatePostReturns200() throws Exception {
        AdminModeratePostRequest req = new AdminModeratePostRequest(99L, ModerationStatus.APPROVED, null);
        when(postService.moderatePost(eq(60L), any())).thenReturn(pr(60L, 1L, "moderated"));

        mockMvc.perform(patch("/posts/admin/60/moderation").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(60));
    }

    @Test
    void getReportsForAdminReturnsList() throws Exception {
        PostReportResponse report = new PostReportResponse(2L, 80L, 9L, "user",
                "abuse", false, null, null, Instant.now(), Instant.now());
        when(postService.getReportsForAdmin(false)).thenReturn(List.of(report));

        mockMvc.perform(get("/posts/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(2));
    }

    @Test
    void resolveReportReturns200() throws Exception {
        ResolvePostReportRequest req = new ResolvePostReportRequest(99L, false, "resolved");
        PostReportResponse resp = new PostReportResponse(3L, 81L, 9L, "user",
                "abuse", true, "resolved", 99L, Instant.now(), Instant.now());
        when(postService.resolveReport(eq(3L), any())).thenReturn(resp);

        mockMvc.perform(patch("/posts/admin/reports/3/resolve").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(3));
    }

    @Test
    void createPromotionOrderReturns200() throws Exception {
        CreatePromotionOrderRequest req = new CreatePromotionOrderRequest(1L, 5000, 7);
        CreatePromotionOrderResponse resp = new CreatePromotionOrderResponse("key", "order_1", 5000, "INR", 70L, 7);
        when(postService.createPromotionOrder(eq(70L), any())).thenReturn(resp);

        mockMvc.perform(post("/posts/70/promotion/order").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_1"));
    }

    @Test
    void verifyPromotionPaymentReturns200() throws Exception {
        VerifyPromotionPaymentRequest req = new VerifyPromotionPaymentRequest(1L, "order_1", "pay_1", "sig");
        when(postService.verifyPromotionPayment(eq(70L), any())).thenReturn(pr(70L, 1L, "paid"));

        mockMvc.perform(post("/posts/70/promotion/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(70));
    }

    // ---- Helpers ------------------------------------------------------------

    /**
     * Builds a minimal PostResponse. Field order matches the record definition:
     * postId, authorId, authorUsername, authorFullName, authorProfilePicUrl,
     * content, mediaUrls, postType, visibility, moderationStatus, moderationReason,
     * automatedFlagged, likesCount, commentsCount, sharesCount, promoted,
     * promotedUntil, promotionStatus, createdAt, updatedAt
     */
    private PostResponse pr(Long postId, Long authorId, String content) {
        return new PostResponse(
                postId, authorId, null, null, null,
                content, List.of(), PostType.TEXT, PostVisibility.PUBLIC,
                ModerationStatus.APPROVED, null, false,
                0L, 0L, 0L,
                false, null, null,
                Instant.now(), Instant.now()
        );
    }
}
