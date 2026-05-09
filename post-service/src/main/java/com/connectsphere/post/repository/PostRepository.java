package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Post;
import com.connectsphere.post.entity.PostVisibility;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence access for Post data.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    java.util.List<Post> findByAuthorId(Long authorId);
    java.util.List<Post> findByPostIdInAndDeletedFalse(List<Long> postIds);

    Page<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    Page<Post> findByVisibilityAndDeletedFalseOrderByCreatedAtDesc(PostVisibility visibility, Pageable pageable);

    @Query("""
            select p from Post p
            where p.visibility = :visibility
              and p.deleted = false
            order by
              case when p.promoted = true and p.promotedUntil > CURRENT_TIMESTAMP then 0 else 1 end,
              p.createdAt desc
            """)
    Page<Post> findPublicFeedWithPromotedFirst(@Param("visibility") PostVisibility visibility, Pageable pageable);

    @Query("""
            select p from Post p
            where p.visibility = :visibility
              and p.deleted = false
              and p.authorId <> :viewerId
            order by
              case when p.promoted = true and p.promotedUntil > CURRENT_TIMESTAMP then 0 else 1 end,
              p.createdAt desc
            """)
    Page<Post> findPublicFeedExcludingAuthor(
            @Param("visibility") PostVisibility visibility,
            @Param("viewerId") Long viewerId,
            Pageable pageable);

    Page<Post> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    List<Post> findByDeletedFalseOrderByCreatedAtDesc();

    List<Post> findByAutomatedFlaggedTrueAndDeletedFalseOrderByCreatedAtDesc();

    List<Post> findByPromotionStatusAndDeletedFalseOrderByUpdatedAtDesc(String promotionStatus);

    long countByDeletedFalse();

    @Query("""
            select p from Post p
            where p.deleted = false
              and p.visibility = com.connectsphere.post.entity.PostVisibility.PUBLIC
              and lower(p.content) like lower(concat('%', :keyword, '%'))
            order by p.createdAt desc
            """)
    Page<Post> searchPublicPosts(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select p from Post p
            where p.deleted = false
              and (
                    p.authorId = :userId
                    or (
                        p.authorId in :authorIds
                        and p.visibility in (
                            com.connectsphere.post.entity.PostVisibility.PUBLIC,
                            com.connectsphere.post.entity.PostVisibility.FOLLOWERS_ONLY
                        )
                    )
                  )
            order by
              case when p.promoted = true and p.promotedUntil > CURRENT_TIMESTAMP then 0 else 1 end,
              p.createdAt desc
            """)
    Page<Post> findFeedPostsWithFollowees(
            @Param("userId") Long userId,
            @Param("authorIds") List<Long> authorIds,
            Pageable pageable);

    @Query("""
            select p from Post p
            where p.deleted = false
              and (
                    p.authorId = :userId
                    or p.visibility = com.connectsphere.post.entity.PostVisibility.PUBLIC
                  )
            order by
              case when p.promoted = true and p.promotedUntil > CURRENT_TIMESTAMP then 0 else 1 end,
              p.createdAt desc
            """)
    Page<Post> findFeedPostsWithoutFollowees(@Param("userId") Long userId, Pageable pageable);
}
