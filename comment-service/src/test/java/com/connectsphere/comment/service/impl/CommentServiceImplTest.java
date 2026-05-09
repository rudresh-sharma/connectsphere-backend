package com.connectsphere.comment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.comment.client.AuthServiceClient;
import com.connectsphere.comment.client.PostServiceClient;
import com.connectsphere.comment.client.UserSummary;
import com.connectsphere.comment.dto.CommentReportRequest;
import com.connectsphere.comment.dto.CommentReportResponse;
import com.connectsphere.comment.dto.CommentResponse;
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
import feign.FeignException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private CacheManager cacheManager;

    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(commentRepository, commentReportRepository, authServiceClient, postServiceClient, cacheManager);
    }

    @Test
    void addCommentTrimsContentAndIncrementsPostCounter() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId(10L);
            return comment;
        });
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        CommentResponse response = service.addComment(new CreateCommentRequest(13L, 2L, null, "  Nice post  "));

        assertEquals(10L, response.commentId());
        assertEquals("Nice post", response.content());
        assertEquals("anuj", response.authorUsername());
        verify(postServiceClient).updateCounter(
                org.mockito.ArgumentMatchers.eq(13L),
                argThat(request -> "comments".equals(request.counterType()) && request.delta() == 1)
        );
    }

    @Test
    void addReplyUsesParentAndReturnsReplyResponse() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment parent = activeComment(15L, 13L, 1L, null, "Parent");
        when(commentRepository.findById(15L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId(22L);
            return comment;
        });

        CommentResponse response = service.addComment(new CreateCommentRequest(13L, 2L, 15L, "  Reply  "));

        assertEquals(22L, response.commentId());
        assertEquals(15L, response.parentCommentId());
        assertEquals("Reply", response.content());
        assertEquals(0L, response.replyCount());
    }

    @Test
    void addReplyRejectsMissingParent() {
        when(commentRepository.findById(15L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.addComment(new CreateCommentRequest(13L, 2L, 15L, "Reply")));
    }

    @Test
    void addReplyRejectsReplyToReply() {
        Comment parentReply = activeComment(15L, 13L, 1L, 11L, "Nested");
        when(commentRepository.findById(15L)).thenReturn(Optional.of(parentReply));

        assertThrows(BadRequestException.class,
                () -> service.addComment(new CreateCommentRequest(13L, 2L, 15L, "Reply")));
    }

    @Test
    void addReplyRejectsParentFromDifferentPost() {
        Comment parent = activeComment(15L, 99L, 1L, null, "Parent");
        when(commentRepository.findById(15L)).thenReturn(Optional.of(parent));

        assertThrows(BadRequestException.class,
                () -> service.addComment(new CreateCommentRequest(13L, 2L, 15L, "Reply")));
    }

    @Test
    void getRepliesRequiresExistingParent() {
        when(commentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getReplies(77L));
    }

    @Test
    void getRepliesReturnsMappedReplies() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment parent = activeComment(77L, 13L, 1L, null, "Parent");
        Comment reply = activeComment(78L, 13L, 2L, 77L, "Reply");
        when(commentRepository.findById(77L)).thenReturn(Optional.of(parent));
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(77L)).thenReturn(List.of(reply));

        List<CommentResponse> replies = service.getReplies(77L);

        assertEquals(1, replies.size());
        assertEquals(78L, replies.get(0).commentId());
        assertEquals("anuj", replies.get(0).authorUsername());
    }

    @Test
    void getCommentsByPostReturnsTopLevelComments() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        when(commentRepository.findByPostIdAndParentCommentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(13L))
                .thenReturn(List.of(comment));
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        List<CommentResponse> comments = service.getCommentsByPost(13L);

        assertEquals(1, comments.size());
        assertEquals(10L, comments.get(0).commentId());
    }

    @Test
    void getCommentByIdFallsBackWhenAuthorLookupFails() {
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        doThrow(FeignException.class).when(authServiceClient).getUserById(2L);

        CommentResponse response = service.getCommentById(10L);

        assertEquals(10L, response.commentId());
        assertEquals("Root", response.content());
    }

    @Test
    void updateCommentTrimsContentForOwner() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Old");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        CommentResponse response = service.updateComment(10L, 2L, new UpdateCommentRequest("  Updated text  "));

        assertEquals("Updated text", response.content());
        assertEquals("Updated text", comment.getContent());
    }

    @Test
    void updateCommentRejectsNonOwner() {
        Comment comment = activeComment(10L, 13L, 2L, null, "Old");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> service.updateComment(10L, 99L, new UpdateCommentRequest("Updated")));
    }

    @Test
    void deleteCommentSoftDeletesRepliesAndDecrementsCounter() {
        Comment topLevel = activeComment(10L, 13L, 2L, null, "Root");
        Comment reply = activeComment(11L, 13L, 1L, 10L, "Reply");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(topLevel));
        when(commentRepository.findByParentCommentId(10L)).thenReturn(List.of(reply));

        service.deleteComment(10L, 2L);

        assertTrue(topLevel.isDeleted());
        assertTrue(reply.isDeleted());
        verify(commentRepository).saveAll(List.of(reply));
        verify(postServiceClient).updateCounter(
                org.mockito.ArgumentMatchers.eq(13L),
                argThat(request -> "comments".equals(request.counterType()) && request.delta() == -2)
        );
    }

    @Test
    void likeAndUnlikeCommentAdjustCountsWithoutGoingNegative() {
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        service.likeComment(10L);
        assertEquals(1L, comment.getLikesCount());

        service.unlikeComment(10L);
        assertEquals(0L, comment.getLikesCount());

        service.unlikeComment(10L);
        assertEquals(0L, comment.getLikesCount());
    }

    @Test
    void deleteCommentsByAuthorDeletesTopLevelAndRepliesOnce() {
        Comment topLevel = activeComment(10L, 13L, 2L, null, "Root");
        Comment reply = activeComment(11L, 13L, 2L, 10L, "Reply");
        when(commentRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(2L)).thenReturn(List.of(reply, topLevel));
        when(commentRepository.findByParentCommentId(10L)).thenReturn(List.of(reply));

        service.deleteCommentsByAuthor(2L);

        assertTrue(topLevel.isDeleted());
        assertTrue(reply.isDeleted());
        verify(postServiceClient, times(2)).updateCounter(
                org.mockito.ArgumentMatchers.eq(13L),
                argThat(request -> "comments".equals(request.counterType()) && request.delta() == -1)
        );
    }

    @Test
    void getCommentsByUserReturnsMappedComments() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        when(commentRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(2L)).thenReturn(List.of(comment));
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        List<CommentResponse> comments = service.getCommentsByUser(2L);

        assertEquals(1, comments.size());
        assertEquals("anuj", comments.get(0).authorUsername());
    }

    @Test
    void getCommentCountDelegatesToRepository() {
        when(commentRepository.countByPostIdAndDeletedFalse(13L)).thenReturn(5L);

        assertEquals(5L, service.getCommentCount(13L));
    }

    @Test
    void adminUpdateCommentRequiresAdminAndTrimsContent() {
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "admin", "ADMIN"));
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Old");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        CommentResponse response = service.adminUpdateComment(10L, 7L, new UpdateCommentRequest("  Admin edit "));

        assertEquals("Admin edit", response.content());
        assertEquals("Admin edit", comment.getContent());
    }

    @Test
    void adminUpdateCommentRejectsNonAdminUser() {
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "mod", "USER"));

        assertThrows(ForbiddenException.class,
                () -> service.adminUpdateComment(10L, 7L, new UpdateCommentRequest("Admin edit")));
    }

    @Test
    void adminDeleteCommentMarksOpenReportsResolved() {
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "admin", "ADMIN"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        Comment reply = activeComment(11L, 13L, 1L, 10L, "Reply");
        CommentReport openReport = CommentReport.builder()
                .reportId(44L)
                .commentId(10L)
                .reporterId(9L)
                .reason("spam")
                .resolved(false)
                .build();
        CommentReport otherReport = CommentReport.builder()
                .reportId(45L)
                .commentId(99L)
                .reporterId(9L)
                .reason("other")
                .resolved(false)
                .build();

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentCommentId(10L)).thenReturn(List.of(reply));
        when(commentReportRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of(openReport, otherReport));

        service.adminDeleteComment(10L, 7L);

        assertTrue(comment.isDeleted());
        assertTrue(reply.isDeleted());
        assertTrue(openReport.isResolved());
        assertEquals(7L, openReport.getResolvedBy());
        assertEquals("Comment removed by admin", openReport.getResolutionNote());
        assertFalse(otherReport.isResolved());
    }

    @Test
    void reportCommentCreatesUnresolvedReportResponse() {
        when(authServiceClient.getUserById(9L)).thenReturn(user(9L, "reporter", "USER"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        CommentReport report = CommentReport.builder()
                .reportId(33L)
                .commentId(10L)
                .reporterId(9L)
                .reason(" abusive ")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentReportRepository.save(any(CommentReport.class))).thenReturn(report);

        CommentReportResponse response = service.reportComment(10L, new CommentReportRequest(9L, " abusive "));

        assertEquals(33L, response.reportId());
        assertEquals("reporter", response.reporterUsername());
        assertEquals(" abusive ", response.reason());
        assertFalse(response.resolved());
    }

    @Test
    void getReportsForAdminReturnsMappedReports() {
        when(authServiceClient.getUserById(9L)).thenReturn(user(9L, "reporter", "USER"));
        CommentReport report = CommentReport.builder()
                .reportId(33L)
                .commentId(10L)
                .reporterId(9L)
                .reason("spam")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(commentReportRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of(report));

        List<CommentReportResponse> reports = service.getReportsForAdmin(false);

        assertEquals(1, reports.size());
        assertEquals("reporter", reports.get(0).reporterUsername());
    }

    @Test
    void resolveReportWithoutRemovingCommentMarksItResolved() {
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "admin", "ADMIN"));
        when(authServiceClient.getUserById(9L)).thenReturn(user(9L, "reporter", "USER"));
        CommentReport report = CommentReport.builder()
                .reportId(33L)
                .commentId(10L)
                .reporterId(9L)
                .reason("spam")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(commentReportRepository.findById(33L)).thenReturn(Optional.of(report));
        when(commentReportRepository.save(report)).thenReturn(report);

        CommentReportResponse response = service.resolveReport(
                33L,
                new ResolveCommentReportRequest(7L, false, "Reviewed")
        );

        assertTrue(report.isResolved());
        assertEquals(7L, report.getResolvedBy());
        assertEquals("Reviewed", report.getResolutionNote());
        assertTrue(response.resolved());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void resolveReportWithRemovalDeletesComment() {
        when(authServiceClient.getUserById(7L)).thenReturn(user(7L, "admin", "ADMIN"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        Comment reply = activeComment(11L, 13L, 1L, 10L, "Reply");
        CommentReport report = CommentReport.builder()
                .reportId(33L)
                .commentId(10L)
                .reporterId(9L)
                .reason("spam")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(commentReportRepository.findById(33L)).thenReturn(Optional.of(report));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentCommentId(10L)).thenReturn(List.of(reply));
        when(commentReportRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of(report));
        when(commentReportRepository.save(report)).thenReturn(report);

        service.resolveReport(33L, new ResolveCommentReportRequest(7L, true, "Removed"));

        assertTrue(comment.isDeleted());
        assertTrue(reply.isDeleted());
        assertTrue(report.isResolved());
        verify(postServiceClient).updateCounter(
                org.mockito.ArgumentMatchers.eq(13L),
                argThat(request -> "comments".equals(request.counterType()) && request.delta() == -2)
        );
    }

    @Test
    void getAllCommentsForAdminReturnsMappedComments() {
        when(authServiceClient.getUserById(2L)).thenReturn(user(2L, "anuj", "USER"));
        Comment comment = activeComment(10L, 13L, 2L, null, "Root");
        when(commentRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(comment));
        when(commentRepository.findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        List<CommentResponse> comments = service.getAllCommentsForAdmin();

        assertEquals(1, comments.size());
        assertEquals("anuj", comments.get(0).authorUsername());
        assertEquals("Root", comments.get(0).content());
    }

    private static Comment activeComment(Long commentId, Long postId, Long authorId, Long parentCommentId, String content) {
        return Comment.builder()
                .commentId(commentId)
                .postId(postId)
                .authorId(authorId)
                .parentCommentId(parentCommentId)
                .content(content)
                .deleted(false)
                .likesCount(0)
                .build();
    }

    private static UserSummary user(Long userId, String username, String role) {
        return new UserSummary(userId, username, username.toUpperCase(), null, username + "@example.com", role, true);
    }
}
