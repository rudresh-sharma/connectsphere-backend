package com.connectsphere.follow.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.follow.client.AuthServiceClient;
import com.connectsphere.follow.client.NotificationClient;
import com.connectsphere.follow.client.UserSummary;
import com.connectsphere.follow.dto.FollowCountsResponse;
import com.connectsphere.follow.dto.FollowStatusResponse;
import com.connectsphere.follow.dto.SuggestedUserResponse;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.exception.BadRequestException;
import com.connectsphere.follow.exception.ResourceNotFoundException;
import com.connectsphere.follow.repository.FollowRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuthServiceClient authServiceClient;

    private FollowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FollowServiceImpl(followRepository, cacheManager, notificationClient, authServiceClient);
    }

    @Test
    void followRejectsSelfFollow() {
        assertThrows(BadRequestException.class, () -> service.follow(10L, 10L));
    }

    @Test
    void followRejectsMissingUserIds() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.follow(null, 2L));

        assertEquals("followerId and followingId are required", exception.getMessage());
    }

    @Test
    void followRejectsAlreadyFollowedUser() {
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.follow(1L, 2L));

        assertEquals("User is already followed", exception.getMessage());
    }

    @Test
    void followCreatesRelationship() {
        Follow saved = follow(99L, 1L, 2L);
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(saved);
        when(authServiceClient.getUserById(1L)).thenReturn(
                new UserSummary(1L, "alice", "Alice", null, true)
        );

        var response = service.follow(1L, 2L);

        assertEquals(99L, response.followId());
        assertEquals(1L, response.followerId());
        assertEquals(2L, response.followingId());
        assertNotNull(response.createdAt());
        verify(notificationClient).sendFollowNotification(2L, 1L, "alice");
    }

    @Test
    void unfollowRequiresExistingRelationship() {
        when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.unfollow(1L, 2L));

        assertEquals("Follow relationship not found", exception.getMessage());
    }

    @Test
    void unfollowDeletesExistingRelationship() {
        Follow existing = follow(50L, 1L, 2L);
        when(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(existing));

        service.unfollow(1L, 2L);

        verify(followRepository).delete(existing);
    }

    @Test
    void getFollowingMapsRepositoryPage() {
        Follow follow = follow(90L, 1L, 2L);
        when(followRepository.findByFollowerIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(follow)));

        Page<?> page = service.getFollowing(1L, PageRequest.of(0, 5));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getFollowersMapsRepositoryPage() {
        Follow follow = follow(90L, 3L, 1L);
        when(followRepository.findByFollowingIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(follow)));

        Page<?> page = service.getFollowers(1L, PageRequest.of(0, 5));

        assertEquals(1, page.getContent().size());
    }

    @Test
    void getStatusDelegatesToRepository() {
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        FollowStatusResponse response = service.getStatus(1L, 2L);

        assertTrue(response.following());
        assertEquals(1L, response.followerId());
        assertEquals(2L, response.followingId());
    }

    @Test
    void getCountsReturnsFollowerAndFollowingCounts() {
        when(followRepository.countByFollowingId(2L)).thenReturn(7L);
        when(followRepository.countByFollowerId(2L)).thenReturn(4L);

        FollowCountsResponse response = service.getCounts(2L);

        assertEquals(7L, response.followersCount());
        assertEquals(4L, response.followingCount());
    }

    @Test
    void getFollowingIdsReturnsFollowedUserIds() {
        when(followRepository.findByFollowerIdOrderByCreatedAtDesc(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(
                        follow(1L, 1L, 2L),
                        follow(2L, 1L, 3L)
                )));

        assertEquals(List.of(2L, 3L), service.getFollowingIds(1L));
    }

    @Test
    void getMutualFollowingIdsRejectsInvalidUsers() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.getMutualFollowingIds(1L, 1L));

        assertEquals("Users cannot follow themselves", exception.getMessage());
    }

    @Test
    void getMutualFollowingIdsDelegatesToRepository() {
        when(followRepository.findMutualFollowingIds(1L, 2L)).thenReturn(List.of(7L, 8L));

        assertEquals(List.of(7L, 8L), service.getMutualFollowingIds(1L, 2L));
    }

    @Test
    void getSuggestedUsersRejectsInvalidUserId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.getSuggestedUsers(0L, PageRequest.of(0, 10)));

        assertEquals("userId is required", exception.getMessage());
    }

    @Test
    void getSuggestedUsersExcludesCurrentAndAlreadyFollowedUsers() {
        Follow existingFollow = follow(10L, 1L, 2L);
        when(followRepository.findByFollowerIdOrderByCreatedAtDesc(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(existingFollow)));
        when(followRepository.findSuggestedUserIds(eq(1L), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.<Object[]>of(new Object[] {3L, 2L})));

        Page<SuggestedUserResponse> result = service.getSuggestedUsers(1L, PageRequest.of(0, 10));

        assertEquals(3L, result.getContent().get(0).userId());
        assertEquals(2L, result.getContent().get(0).mutualConnections());
        verify(followRepository).findSuggestedUserIds(
                eq(1L),
                org.mockito.ArgumentMatchers.argThat(ids -> ids.containsAll(List.of(1L, 2L))),
                any(Pageable.class)
        );
    }

    @Test
    void deleteUserRelationshipsRequiresUserId() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.deleteUserRelationships(null));

        assertEquals("userId is required", exception.getMessage());
        verify(followRepository, never()).deleteByFollowerIdOrFollowingId(any(), any());
    }

    @Test
    void deleteUserRelationshipsDeletesByFollowerAndFollowing() {
        service.deleteUserRelationships(9L);

        verify(followRepository).deleteByFollowerIdOrFollowingId(9L, 9L);
    }

    private static Follow follow(Long followId, Long followerId, Long followingId) {
        Follow follow = new Follow();
        follow.setFollowId(followId);
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setCreatedAt(Instant.now());
        return follow;
    }
}
