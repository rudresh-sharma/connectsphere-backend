package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Story;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence access for Story data.
 */
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Instant now);
    List<Story> findByAuthorIdInAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Collection<Long> authorIds, Instant now);
    List<Story> findByAuthorIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Long authorId, Instant now);
    List<Story> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @Modifying
    @Query("UPDATE Story s SET s.active = false WHERE s.active = true AND s.expiresAt < :now")
    int expireOldStories(@Param("now") Instant now);
}
