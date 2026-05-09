package com.connectsphere.like.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.like.client.PostServiceClient;
import com.connectsphere.like.dto.ChangeReactionRequest;
import com.connectsphere.like.dto.LikeRequest;
import com.connectsphere.like.dto.LikeResponse;
import com.connectsphere.like.dto.ReactionSummary;
import com.connectsphere.like.entity.Like;
import com.connectsphere.like.entity.ReactionType;
import com.connectsphere.like.entity.TargetType;
import com.connectsphere.like.exception.BadRequestException;
import com.connectsphere.like.exception.ResourceNotFoundException;
import com.connectsphere.like.repository.LikeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private CacheManager cacheManager;

    private LikeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LikeServiceImpl(likeRepository, postServiceClient, cacheManager);
    }

    @Test
    void likeTargetDefaultsReactionAndIncrementsPostCounter() {
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setLikeId(5L);
            like.setCreatedAt(Instant.now());
            return like;
        });

        LikeResponse response = service.likeTarget(new LikeRequest(1L, 13L, TargetType.POST, null));

        assertEquals(ReactionType.LIKE, response.reactionType());
        verify(postServiceClient).updateCounter(
                org.mockito.ArgumentMatchers.eq(13L),
                argThat(request -> "likes".equals(request.counterType()) && request.delta() == 1)
        );
    }

    @Test
    void likeTargetRejectsDuplicateReaction() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(1L, 13L, TargetType.POST)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.likeTarget(new LikeRequest(1L, 13L, TargetType.POST, ReactionType.LOVE)));

        assertEquals("User has already reacted to this target", exception.getMessage());
    }

    @Test
    void likeTargetForCommentDoesNotCallPostCounter() {
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setLikeId(7L);
            like.setCreatedAt(Instant.now());
            return like;
        });

        LikeResponse response = service.likeTarget(new LikeRequest(2L, 99L, TargetType.COMMENT, ReactionType.WOW));

        assertEquals(ReactionType.WOW, response.reactionType());
        verify(postServiceClient, never()).updateCounter(any(), any());
    }

    @Test
    void unlikeTargetDeletesExistingPostLikeAndDecrementsCounter() {
        Like existing = like(9L, 1L, 13L, TargetType.POST, ReactionType.LIKE);
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(1L, 13L, TargetType.POST)).thenReturn(Optional.of(existing));

        service.unlikeTarget(1L, 13L, TargetType.POST);

        verify(likeRepository).delete(existing);
        verify(postServiceClient).updateCounter(
                org.mockito.ArgumentMatchers.eq(13L),
                argThat(request -> "likes".equals(request.counterType()) && request.delta() == -1)
        );
    }

    @Test
    void unlikeTargetRejectsMissingLike() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(1L, 13L, TargetType.POST)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.unlikeTarget(1L, 13L, TargetType.POST));

        assertEquals("Like not found", exception.getMessage());
    }

    @Test
    void hasLikedDelegatesToRepository() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(3L, 22L, TargetType.COMMENT)).thenReturn(true);

        assertEquals(true, service.hasLiked(3L, 22L, TargetType.COMMENT));
    }

    @Test
    void getLikesByTargetMapsResponses() {
        when(likeRepository.findByTargetIdAndTargetType(13L, TargetType.POST)).thenReturn(List.of(
                like(1L, 1L, 13L, TargetType.POST, ReactionType.LIKE),
                like(2L, 2L, 13L, TargetType.POST, ReactionType.LOVE)
        ));

        List<LikeResponse> result = service.getLikesByTarget(13L, TargetType.POST);

        assertEquals(2, result.size());
        assertEquals(ReactionType.LOVE, result.get(1).reactionType());
    }

    @Test
    void getLikesByUserMapsResponses() {
        when(likeRepository.findByUserIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(
                like(8L, 4L, 13L, TargetType.POST, ReactionType.HAHA)
        ));

        List<LikeResponse> result = service.getLikesByUser(4L);

        assertEquals(1, result.size());
        assertEquals(8L, result.get(0).likeId());
    }

    @Test
    void getLikeCountDelegatesToRepository() {
        when(likeRepository.countByTargetIdAndTargetType(13L, TargetType.POST)).thenReturn(5L);

        assertEquals(5L, service.getLikeCount(13L, TargetType.POST));
    }

    @Test
    void getReactionSummaryAggregatesCountsByReactionType() {
        when(likeRepository.findByTargetIdAndTargetType(13L, TargetType.POST)).thenReturn(List.of(
                like(1L, 1L, 13L, TargetType.POST, ReactionType.LIKE),
                like(2L, 2L, 13L, TargetType.POST, ReactionType.LIKE),
                like(3L, 3L, 13L, TargetType.POST, ReactionType.WOW)
        ));

        ReactionSummary summary = service.getReactionSummary(13L, TargetType.POST);

        assertEquals(3L, summary.totalCount());
        assertEquals(2L, summary.counts().get(ReactionType.LIKE));
        assertEquals(1L, summary.counts().get(ReactionType.WOW));
    }

    @Test
    void changeReactionUpdatesExistingLike() {
        Like existing = like(9L, 1L, 13L, TargetType.POST, ReactionType.LIKE);
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(1L, 13L, TargetType.POST)).thenReturn(Optional.of(existing));
        when(likeRepository.save(existing)).thenReturn(existing);

        LikeResponse response = service.changeReaction(1L, 13L, TargetType.POST, new ChangeReactionRequest(ReactionType.SAD));

        assertEquals(ReactionType.SAD, existing.getReactionType());
        assertEquals(ReactionType.SAD, response.reactionType());
    }

    @Test
    void changeReactionRejectsMissingLike() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(1L, 13L, TargetType.POST)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.changeReaction(1L, 13L, TargetType.POST, new ChangeReactionRequest(ReactionType.SAD)));

        assertEquals("Like not found", exception.getMessage());
    }

    @Test
    void deleteLikesByUserRequiresUserId() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.deleteLikesByUser(null));

        assertEquals("userId is required", exception.getMessage());
        verify(likeRepository, never()).deleteByUserId(any());
    }

    @Test
    void deleteLikesByUserDeletesAllUserLikes() {
        when(likeRepository.findByUserIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(
                like(8L, 4L, 13L, TargetType.POST, ReactionType.HAHA),
                like(9L, 4L, 19L, TargetType.COMMENT, ReactionType.LOVE)
        ));

        service.deleteLikesByUser(4L);

        verify(likeRepository).deleteByUserId(4L);
    }

    private static Like like(Long likeId, Long userId, Long targetId, TargetType targetType, ReactionType reactionType) {
        return Like.builder()
                .likeId(likeId)
                .userId(userId)
                .targetId(targetId)
                .targetType(targetType)
                .reactionType(reactionType)
                .createdAt(Instant.now())
                .build();
    }
}
