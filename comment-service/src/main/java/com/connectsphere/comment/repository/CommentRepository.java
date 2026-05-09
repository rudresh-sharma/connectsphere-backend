package com.connectsphere.comment.repository;

import com.connectsphere.comment.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Comment data.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndParentCommentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(Long postId);

    List<Comment> findByParentCommentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentCommentId);

    List<Comment> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(Long authorId);

    List<Comment> findByDeletedFalseOrderByCreatedAtDesc();

    long countByPostIdAndDeletedFalse(Long postId);

    List<Comment> findByParentCommentId(Long parentCommentId);
}
