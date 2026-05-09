package com.connectsphere.comment.service;

import com.connectsphere.comment.dto.CommentResponse;
import com.connectsphere.comment.dto.CommentReportRequest;
import com.connectsphere.comment.dto.CommentReportResponse;
import com.connectsphere.comment.dto.CreateCommentRequest;
import com.connectsphere.comment.dto.ResolveCommentReportRequest;
import com.connectsphere.comment.dto.UpdateCommentRequest;
import java.util.List;

/**
 * Defines Comment business operations.
 */
public interface CommentService {

    CommentResponse addComment(CreateCommentRequest request);

    List<CommentResponse> getCommentsByPost(Long postId);

    CommentResponse getCommentById(Long commentId);

    List<CommentResponse> getReplies(Long commentId);

    CommentResponse updateComment(Long commentId, Long requesterId, UpdateCommentRequest request);

    void deleteComment(Long commentId, Long requesterId);

    List<CommentResponse> getCommentsByUser(Long userId);

    void likeComment(Long commentId);

    void unlikeComment(Long commentId);

    long getCommentCount(Long postId);

    void deleteCommentsByAuthor(Long authorId);

    List<CommentResponse> getAllCommentsForAdmin();

    CommentResponse adminUpdateComment(Long commentId, Long adminUserId, UpdateCommentRequest request);

    void adminDeleteComment(Long commentId, Long adminUserId);

    CommentReportResponse reportComment(Long commentId, CommentReportRequest request);

    List<CommentReportResponse> getReportsForAdmin(boolean resolved);

    CommentReportResponse resolveReport(Long reportId, ResolveCommentReportRequest request);
}
