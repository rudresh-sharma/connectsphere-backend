package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Hashtag data.
 */
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByTag(String tag);

    List<Hashtag> findAllByOrderByPostCountDescLastUsedAtDesc(Pageable pageable);

    List<Hashtag> findByTagContainingIgnoreCase(String keyword);
}
