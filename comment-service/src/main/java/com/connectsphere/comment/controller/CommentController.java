package com.connectsphere.comment.controller;

import com.connectsphere.comment.dto.CommentResponse;
import com.connectsphere.comment.dto.CommentReportRequest;
import com.connectsphere.comment.dto.CommentReportResponse;
import com.connectsphere.comment.dto.CreateCommentRequest;
import com.connectsphere.comment.dto.ResolveCommentReportRequest;
import com.connectsphere.comment.dto.UpdateCommentRequest;
import com.connectsphere.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
/**
 * Exposes Comment API endpoints.
 */


@RestController
@RequestMapping("/comments")

public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
/**
 * Handles the comment request.
 * @param request request payload
 * @return resulting value
 */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@Valid @RequestBody CreateCommentRequest request) {
        return commentService.addComment(request);
    }
/**
 * Returns by post.
 * @param postId entity identifier
 * @return matching results
 */

    @GetMapping("/post/{postId}")
    public List<CommentResponse> getByPost(@PathVariable("postId") Long postId) {
        return commentService.getCommentsByPost(postId);
    }
/**
 * Returns by id.
 * @param commentId entity identifier
 * @return operation result
 */

    @GetMapping("/{commentId}")
    public CommentResponse getById(@PathVariable("commentId") Long commentId) {
        return commentService.getCommentById(commentId);
    }
/**
 * Returns replies.
 * @param commentId entity identifier
 * @return matching results
 */

    @GetMapping("/{commentId}/replies")
    public List<CommentResponse> getReplies(@PathVariable("commentId") Long commentId) {
        return commentService.getReplies(commentId);
    }
/**
 * Updates comment.
 * @param commentId entity identifier
 * @param requesterId entity identifier
 * @param request request payload
 * @return operation result
 */

    @PutMapping("/{commentId}")
    public CommentResponse updateComment(
            @PathVariable("commentId") Long commentId,
            @RequestParam("requesterId") Long requesterId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(commentId, requesterId, request);
    }
/**
 * Deletes comment.
 * @param commentId entity identifier
 * @param requesterId entity identifier
 */

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable("commentId") Long commentId,
            @RequestParam("requesterId") Long requesterId) {
        commentService.deleteComment(commentId, requesterId);
    }
/**
 * Handles the like comment request.
 * @param commentId entity identifier
 */

    @PostMapping("/{commentId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeComment(@PathVariable("commentId") Long commentId) {
        commentService.likeComment(commentId);
    }
/**
 * Handles the unlike comment request.
 * @param commentId entity identifier
 */

    @PostMapping("/{commentId}/unlike")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikeComment(@PathVariable("commentId") Long commentId) {
        commentService.unlikeComment(commentId);
    }
/**
 * Returns by user.
 * @param userId entity identifier
 * @return matching results
 */

    @GetMapping("/user/{userId}")
    public List<CommentResponse> getByUser(@PathVariable("userId") Long userId) {
        return commentService.getCommentsByUser(userId);
    }
/**
 * Returns comment count.
 * @param postId entity identifier
 * @return operation result
 */

    @GetMapping("/post/{postId}/count")
    public long getCommentCount(@PathVariable("postId") Long postId) {
        return commentService.getCommentCount(postId);
    }
/**
 * Reports comment.
 * @param commentId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @PostMapping("/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentReportResponse reportComment(
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody CommentReportRequest request
    ) {
        return commentService.reportComment(commentId, request);
    }
/**
 * Returns all comments for admin.
 * @return matching results
 */

    @GetMapping("/admin/all")
    public List<CommentResponse> getAllCommentsForAdmin() {
        return commentService.getAllCommentsForAdmin();
    }
/**
 * Handles the admin update comment request.
 * @param commentId entity identifier
 * @param adminUserId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @PutMapping("/admin/{commentId}")
    public CommentResponse adminUpdateComment(
            @PathVariable("commentId") Long commentId,
            @RequestParam("adminUserId") Long adminUserId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return commentService.adminUpdateComment(commentId, adminUserId, request);
    }
/**
 * Handles the admin delete comment request.
 * @param commentId entity identifier
 * @param adminUserId entity identifier
 */

    @DeleteMapping("/admin/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDeleteComment(
            @PathVariable("commentId") Long commentId,
            @RequestParam("adminUserId") Long adminUserId
    ) {
        commentService.adminDeleteComment(commentId, adminUserId);
    }
/**
 * Returns reports for admin.
 * @param "resolved" method input parameter
 * @param resolved method input parameter
 * @return matching results
 */

    @GetMapping("/admin/reports")
    public List<CommentReportResponse> getReportsForAdmin(@RequestParam(value = "resolved", defaultValue = "false") boolean resolved) {
        return commentService.getReportsForAdmin(resolved);
    }
/**
 * Handles the report request.
 * @param reportId entity identifier
 * @param request request payload
 * @return operation result
 */

    @PatchMapping("/admin/reports/{reportId}/resolve")
    public CommentReportResponse resolveReport(
            @PathVariable("reportId") Long reportId,
            @Valid @RequestBody ResolveCommentReportRequest request
    ) {
        return commentService.resolveReport(reportId, request);
    }
/**
 * Deletes comments by author.
 * @param userId entity identifier
 */

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentsByAuthor(@PathVariable("userId") Long userId) {
        commentService.deleteCommentsByAuthor(userId);
    }
}
