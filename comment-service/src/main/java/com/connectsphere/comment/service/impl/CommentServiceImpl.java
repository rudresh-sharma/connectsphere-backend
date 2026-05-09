package com.connectsphere.comment.service.impl;

import com.connectsphere.comment.client.AuthServiceClient;
import com.connectsphere.comment.client.PostServiceClient;
import com.connectsphere.comment.client.UserSummary;
import com.connectsphere.comment.config.CacheConfig;
import com.connectsphere.comment.dto.CommentResponse;
import com.connectsphere.comment.dto.CommentReportRequest;
import com.connectsphere.comment.dto.CommentReportResponse;
import com.connectsphere.comment.dto.CreateCommentRequest;
import com.connectsphere.comment.dto.ResolveCommentReportRequest;
import com.connectsphere.comment.dto.UpdateCommentRequest;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.entity.CommentReport;
import com.connectsphere.comment.exception.BadRequestException;
import com.connectsphere.comment.exception.ForbiddenException;
import com.connectsphere.comment.exception.ResourceNotFoundException;
import com.connectsphere.comment.repository.CommentReportRepository;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.service.CommentService;
import com.connectsphere.comment.util.CommentMapper;
import feign.FeignException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Implements Comment business operations.
 */


@Service
@RequiredArgsConstructor
@Transactional

public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final AuthServiceClient authServiceClient;
    private final PostServiceClient postServiceClient;
    private final CacheManager cacheManager;
/**
 * Adds comment.
 * @param request request payload
 * @return resulting value
 */

    @Override
    public CommentResponse addComment(CreateCommentRequest request) {
        // Validate parent comment if this is a reply
        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));

            if (!parent.isTopLevel()) {
                throw new BadRequestException("Replies to replies are not allowed (two-level threading only)");
            }

            if (!parent.getPostId().equals(request.postId())) {
                throw new BadRequestException("Parent comment does not belong to the specified post");
            }
        }

        Comment comment = Comment.builder()
                .postId(request.postId())
                .authorId(request.authorId())
                .parentCommentId(request.parentCommentId())
                .content(request.content().trim())
                .build();

        Comment saved = commentRepository.save(comment);
        evictCommentCaches(saved);

        // Increment comments count on the post
        incrementPostCommentCounter(request.postId(), 1);

        return toResponse(saved);
    }
/**
 * Returns comments by post.
 * @param postId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        return commentRepository
                .findByPostIdAndParentCommentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Returns comment by id.
 * @param commentId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long commentId) {
        return toResponse(findActiveComment(commentId));
    }
/**
 * Returns replies.
 * @param commentId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(Long commentId) {
        // Ensure the parent comment exists
        findActiveComment(commentId);

        return commentRepository
                .findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(commentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Updates comment.
 * @param commentId entity identifier
 * @param requesterId entity identifier
 * @param request request payload
 * @return operation result
 */

    @Override
    public CommentResponse updateComment(Long commentId, Long requesterId, UpdateCommentRequest request) {
        Comment comment = findActiveComment(commentId);
        assertOwner(comment, requesterId);
        comment.setContent(request.content().trim());
        Comment saved = commentRepository.save(comment);
        evictCommentCaches(saved);
        return toResponse(saved);
    }
/**
 * Deletes comment.
 * @param commentId entity identifier
 * @param requesterId entity identifier
 */

    @Override
    public void deleteComment(Long commentId, Long requesterId) {
        Comment comment = findActiveComment(commentId);
        assertOwner(comment, requesterId);

        long deletedCount = 0;

        // If top-level comment, also soft-delete all replies
        if (comment.isTopLevel()) {
            List<Comment> replies = commentRepository.findByParentCommentId(commentId);
            for (Comment reply : replies) {
                if (!reply.isDeleted()) {
                    reply.setDeleted(true);
                    evictCommentCaches(reply);
                    deletedCount++;
                }
            }
            commentRepository.saveAll(replies);
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
        evictCommentCaches(comment);
        deletedCount++; // count the comment itself

        // Decrement comments count on the post
        incrementPostCommentCounter(comment.getPostId(), -deletedCount);
    }
/**
 * Returns comments by user.
 * @param userId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByUser(Long userId) {
        return commentRepository
                .findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Performs the like comment operation.
 * @param commentId entity identifier
 */

    @Override
    public void likeComment(Long commentId) {
        Comment comment = findActiveComment(commentId);
        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
        evictCommentCaches(comment);
    }
/**
 * Performs the unlike comment operation.
 * @param commentId entity identifier
 */

    @Override
    public void unlikeComment(Long commentId) {
        Comment comment = findActiveComment(commentId);
        if (comment.getLikesCount() > 0) {
            comment.setLikesCount(comment.getLikesCount() - 1);
            commentRepository.save(comment);
            evictCommentCaches(comment);
        }
    }
/**
 * Returns comment count.
 * @param postId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(Long postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }
/**
 * Deletes comments by author.
 * @param authorId entity identifier
 */

    @Override
    public void deleteCommentsByAuthor(Long authorId) {
        if (authorId == null) {
            throw new BadRequestException("authorId is required");
        }

        List<Comment> comments = commentRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(authorId);
        Set<Long> processedCommentIds = new HashSet<>();

        for (Comment comment : comments) {
            if (comment.isDeleted() || !processedCommentIds.add(comment.getCommentId())) {
                continue;
            }

            long deletedCount = 1;

            if (comment.isTopLevel()) {
                List<Comment> replies = commentRepository.findByParentCommentId(comment.getCommentId());
                for (Comment reply : replies) {
                    if (!reply.isDeleted() && processedCommentIds.add(reply.getCommentId())) {
                        reply.setDeleted(true);
                        evictCommentCaches(reply);
                        deletedCount++;
                    }
                }
                commentRepository.saveAll(replies);
            }

            comment.setDeleted(true);
            commentRepository.save(comment);
            evictCommentCaches(comment);
            incrementPostCommentCounter(comment.getPostId(), -deletedCount);
        }
    }
/**
 * Returns all comments for admin.
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getAllCommentsForAdmin() {
        return commentRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }
/**
 * Performs the admin update comment operation.
 * @param commentId entity identifier
 * @param adminUserId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @Override
    public CommentResponse adminUpdateComment(Long commentId, Long adminUserId, UpdateCommentRequest request) {
        assertAdmin(adminUserId);
        Comment comment = findActiveComment(commentId);
        comment.setContent(request.content().trim());
        Comment saved = commentRepository.save(comment);
        evictCommentCaches(saved);
        return toResponse(saved);
    }
/**
 * Performs the admin delete comment operation.
 * @param commentId entity identifier
 * @param adminUserId entity identifier
 */

    @Override
    public void adminDeleteComment(Long commentId, Long adminUserId) {
        assertAdmin(adminUserId);
        Comment comment = findActiveComment(commentId);
        deleteCommentHierarchy(comment, adminUserId);
    }
/**
 * Reports comment.
 * @param commentId entity identifier
 * @param request request payload
 * @return resulting value
 */

    @Override
    public CommentReportResponse reportComment(Long commentId, CommentReportRequest request) {
        findActiveComment(commentId);
        CommentReport report = commentReportRepository.save(CommentReport.builder()
                .commentId(commentId)
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
    public List<CommentReportResponse> getReportsForAdmin(boolean resolved) {
        return commentReportRepository.findByResolvedOrderByCreatedAtDesc(resolved).stream()
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
    public CommentReportResponse resolveReport(Long reportId, ResolveCommentReportRequest request) {
        assertAdmin(request.adminUserId());
        CommentReport report = commentReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setResolved(true);
        report.setResolvedBy(request.adminUserId());
        report.setResolutionNote(request.resolutionNote());

        if (request.removeComment()) {
            Comment comment = findActiveComment(report.getCommentId());
            deleteCommentHierarchy(comment, request.adminUserId());
        }

        return toReportResponse(commentReportRepository.save(report));
    }

    // ---- helpers ----

    private Comment findActiveComment(Long commentId) {
        return commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private void assertOwner(Comment comment, Long requesterId) {
        if (requesterId == null) {
            throw new BadRequestException("requesterId is required");
        }
        if (!comment.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("You can only modify your own comments");
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

    private void deleteCommentHierarchy(Comment comment, Long actorId) {
        long deletedCount = 0;

        if (comment.isTopLevel()) {
            List<Comment> replies = commentRepository.findByParentCommentId(comment.getCommentId());
            for (Comment reply : replies) {
                if (!reply.isDeleted()) {
                    reply.setDeleted(true);
                    evictCommentCaches(reply);
                    deletedCount++;
                }
            }
            commentRepository.saveAll(replies);
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
        evictCommentCaches(comment);
        deletedCount++;
        incrementPostCommentCounter(comment.getPostId(), -deletedCount);

        commentReportRepository.findByResolvedOrderByCreatedAtDesc(false).stream()
                .filter(report -> comment.getCommentId().equals(report.getCommentId()))
                .forEach(report -> {
                    report.setResolved(true);
                    report.setResolvedBy(actorId);
                    report.setResolutionNote("Comment removed by admin");
                });
    }

    private void incrementPostCommentCounter(Long postId, long delta) {
        try {
            postServiceClient.updateCounter(postId, new PostServiceClient.CounterRequest("comments", delta));
        } catch (FeignException ex) {
            log.warn("Could not update comment counter on post-service for postId={}: status={}, message={}",
                    postId, ex.status(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Could not update comment counter on post-service for postId={}", postId, ex);
        }
    }

    private CommentResponse toResponse(Comment comment) {
        UserSummary author = null;
        try {
            author = authServiceClient.getUserById(comment.getAuthorId());
        } catch (FeignException ex) {
            log.warn("Could not load author data for userId={} while rendering commentId={}: status={}, message={}",
                    comment.getAuthorId(), comment.getCommentId(), ex.status(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Could not load author data for userId={} while rendering commentId={}",
                    comment.getAuthorId(), comment.getCommentId(), ex);
        }

        long replyCount = 0;
        if (comment.isTopLevel()) {
            replyCount = commentRepository
                    .findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(comment.getCommentId())
                    .size();
        }

        return CommentMapper.toResponse(comment, author, replyCount);
    }

    private void evictCommentCaches(Comment comment) {
        evict(CacheConfig.COMMENT_DETAILS_CACHE, comment.getCommentId());
        evict(CacheConfig.POST_COMMENTS_CACHE, comment.getPostId());
        evict(CacheConfig.COMMENT_COUNTS_CACHE, comment.getPostId());
        if (comment.isTopLevel()) {
            evict(CacheConfig.COMMENT_REPLIES_CACHE, comment.getCommentId());
        } else {
            evict(CacheConfig.COMMENT_REPLIES_CACHE, comment.getParentCommentId());
            evict(CacheConfig.COMMENT_DETAILS_CACHE, comment.getParentCommentId());
        }
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    private CommentReportResponse toReportResponse(CommentReport report) {
        UserSummary reporter = null;
        try {
            reporter = authServiceClient.getUserById(report.getReporterId());
        } catch (FeignException ex) {
            log.warn("Could not load reporter data for comment reportId={}: status={}, message={}",
                    report.getReportId(), ex.status(), ex.getMessage());
        }

        return new CommentReportResponse(
                report.getReportId(),
                report.getCommentId(),
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
}
