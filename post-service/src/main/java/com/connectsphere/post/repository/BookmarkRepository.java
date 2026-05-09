package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Bookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence access for Bookmark data.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);

    @Query("""
            select b.postId from Bookmark b
            where b.userId = :userId
            order by b.createdAt desc
            """)
    List<Long> findPostIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Modifying
    void deleteByPostId(Long postId);

    @Modifying
    void deleteByPostIdIn(List<Long> postIds);

    @Modifying
    void deleteByUserId(Long userId);
}
