package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Reel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Reel data.
 */
public interface ReelRepository extends JpaRepository<Reel, Long> {
    List<Reel> findByActiveTrueOrderByCreatedAtDesc();
    List<Reel> findByAuthorIdAndActiveTrueOrderByCreatedAtDesc(Long authorId);
    List<Reel> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
