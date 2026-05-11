package com.connectsphere.like.repository;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.entity.TargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * Provides persistence access for Like data.
 */
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);

    List<Like> findByTargetIdAndTargetType(Long targetId, TargetType targetType);

    List<Like> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);

    long countByTargetIdAndTargetType(Long targetId, TargetType targetType);
    
    void deleteByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);

    @Modifying
    void deleteByUserId(Long userId);
}
