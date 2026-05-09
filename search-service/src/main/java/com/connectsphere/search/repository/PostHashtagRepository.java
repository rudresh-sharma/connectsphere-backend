package com.connectsphere.search.repository;

import com.connectsphere.search.entity.PostHashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Post Hashtag data.
 */
public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {
    List<PostHashtag> findByPostId(Long postId);
    List<PostHashtag> findByHashtagId(Long hashtagId);
    void deleteByPostId(Long postId);
    boolean existsByPostIdAndHashtagId(Long postId, Long hashtagId);
}
