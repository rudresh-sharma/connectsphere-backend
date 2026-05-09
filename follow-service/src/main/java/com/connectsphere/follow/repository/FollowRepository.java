package com.connectsphere.follow.repository;

import com.connectsphere.follow.entity.Follow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence access for Follow data.
 */
public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<Follow> findByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);

    Page<Follow> findByFollowingIdOrderByCreatedAtDesc(Long followingId, Pageable pageable);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    Page<Follow> findByFollowerIdNotAndFollowingIdNotIn(Long followerId, Collection<Long> excludedFollowingIds, Pageable pageable);

    @Query("""
            select f.followingId from Follow f
            where f.followerId = :userId
              and f.followingId in (
                  select other.followingId from Follow other
                  where other.followerId = :otherUserId
              )
            order by f.followingId
            """)
    List<Long> findMutualFollowingIds(
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId);

    @Query("""
            select second.followingId, count(second.followingId)
            from Follow first
            join Follow second on second.followerId = first.followingId
            where first.followerId = :userId
              and second.followingId <> :userId
              and second.followingId not in :excludedUserIds
            group by second.followingId
            order by count(second.followingId) desc, second.followingId asc
            """)
    Page<Object[]> findSuggestedUserIds(
            @Param("userId") Long userId,
            @Param("excludedUserIds") Collection<Long> excludedUserIds,
            Pageable pageable);

    void deleteByFollowerIdOrFollowingId(Long followerId, Long followingId);
}
