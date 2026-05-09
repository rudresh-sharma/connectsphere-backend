package com.connectsphere.search.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.search.dto.HashtagResponse;
import com.connectsphere.search.dto.IndexPostRequest;
import com.connectsphere.search.dto.PostSearchResult;
import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostDocument;
import com.connectsphere.search.entity.PostHashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.PostHashtagRepository;
import com.connectsphere.search.repository.PostSearchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private PostHashtagRepository postHashtagRepository;

    @Mock
    private PostSearchRepository postSearchRepository;

    @Mock
    private CacheManager cacheManager;

    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchServiceImpl(hashtagRepository, postHashtagRepository, postSearchRepository, cacheManager);
    }

    @Test
    void indexPostExtractsLowercaseHashtagsAndCreatesMappings() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.empty());
        when(hashtagRepository.save(any(Hashtag.class))).thenAnswer(invocation -> {
            Hashtag hashtag = invocation.getArgument(0);
            if (hashtag.getHashtagId() == null) {
                hashtag.setHashtagId(4L);
            }
            return hashtag;
        });

        service.indexPost(new IndexPostRequest(13L, 2L, "Learning #Java today", "anuj", "PUBLIC"));

        verify(postSearchRepository).save(any(PostDocument.class));
        verify(postHashtagRepository).existsByPostIdAndHashtagId(13L, 4L);
        verify(postHashtagRepository).save(any(PostHashtag.class));
    }

    @Test
    void indexPostUpdatesExistingHashtagAndSkipsDuplicateMapping() {
        Hashtag hashtag = Hashtag.builder().hashtagId(4L).tag("java").postCount(2).lastUsedAt(Instant.now()).build();
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.of(hashtag));
        when(postHashtagRepository.existsByPostIdAndHashtagId(13L, 4L)).thenReturn(true);

        service.indexPost(new IndexPostRequest(13L, 2L, "Learning #Java today", "anuj", "PUBLIC"));

        verify(postHashtagRepository).existsByPostIdAndHashtagId(13L, 4L);
        verify(postHashtagRepository, org.mockito.Mockito.never()).save(any(PostHashtag.class));
    }

    @Test
    void indexPostContinuesWhenElasticIndexingFails() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.empty());
        when(hashtagRepository.save(any(Hashtag.class))).thenAnswer(invocation -> {
            Hashtag hashtag = invocation.getArgument(0);
            if (hashtag.getHashtagId() == null) {
                hashtag.setHashtagId(4L);
            }
            return hashtag;
        });
        doThrow(new RuntimeException("elastic down")).when(postSearchRepository).save(any(PostDocument.class));

        service.indexPost(new IndexPostRequest(13L, 2L, "Learning #Java today", "anuj", "PUBLIC"));

        verify(postHashtagRepository).save(any(PostHashtag.class));
    }

    @Test
    void removePostIndexDecrementsHashtagCountsAndDeletesMappings() {
        PostHashtag mapping = PostHashtag.builder().id(1L).postId(13L).hashtagId(4L).build();
        Hashtag hashtag = Hashtag.builder().hashtagId(4L).tag("java").postCount(3).lastUsedAt(Instant.now()).build();
        when(postHashtagRepository.findByPostId(13L)).thenReturn(List.of(mapping));
        when(hashtagRepository.findById(4L)).thenReturn(Optional.of(hashtag));

        service.removePostIndex(13L);

        assertEquals(2L, hashtag.getPostCount());
        verify(postSearchRepository).deleteById("13");
        verify(postHashtagRepository).deleteByPostId(13L);
        verify(hashtagRepository).save(hashtag);
    }

    @Test
    void removePostIndexContinuesWhenElasticDeleteFails() {
        doThrow(new RuntimeException("elastic down")).when(postSearchRepository).deleteById("13");
        when(postHashtagRepository.findByPostId(13L)).thenReturn(List.of());

        service.removePostIndex(13L);

        verify(postHashtagRepository).deleteByPostId(13L);
    }

    @Test
    void searchPostsMapsDocumentsToResults() {
        PostDocument document = PostDocument.builder()
                .postId(13L)
                .authorId(2L)
                .content("hello")
                .authorUsername("anuj")
                .visibility("PUBLIC")
                .build();
        when(postSearchRepository.findByContentContaining("hello")).thenReturn(List.of(document));

        List<PostSearchResult> results = service.searchPosts("hello");

        assertEquals(1, results.size());
        assertEquals(13L, results.get(0).postId());
        assertEquals("anuj", results.get(0).authorUsername());
    }

    @Test
    void searchPostsReturnsEmptyListWhenRepositoryFails() {
        doThrow(new RuntimeException("elastic down")).when(postSearchRepository).findByContentContaining("hello");

        assertTrue(service.searchPosts("hello").isEmpty());
    }

    @Test
    void getHashtagsForPostReturnsResolvedHashtags() {
        PostHashtag mapping = PostHashtag.builder().id(1L).postId(13L).hashtagId(4L).build();
        Hashtag hashtag = Hashtag.builder().hashtagId(4L).tag("java").postCount(3).lastUsedAt(Instant.now()).build();
        when(postHashtagRepository.findByPostId(13L)).thenReturn(List.of(mapping));
        when(hashtagRepository.findById(4L)).thenReturn(Optional.of(hashtag));

        List<HashtagResponse> results = service.getHashtagsForPost(13L);

        assertEquals(1, results.size());
        assertEquals("java", results.get(0).tag());
    }

    @Test
    void getTrendingHashtagsClampsLimit() {
        Hashtag hashtag = Hashtag.builder().hashtagId(4L).tag("java").postCount(3).lastUsedAt(Instant.now()).build();
        when(hashtagRepository.findAllByOrderByPostCountDescLastUsedAtDesc(PageRequest.of(0, 100)))
                .thenReturn(List.of(hashtag));

        List<HashtagResponse> results = service.getTrendingHashtags(200);

        assertEquals(1, results.size());
        assertEquals("java", results.get(0).tag());
    }

    @Test
    void getPostsByHashtagReturnsEmptyWhenHashtagMissing() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.empty());

        assertTrue(service.getPostsByHashtag("JAVA").isEmpty());
    }

    @Test
    void getPostsByHashtagReturnsMatchingPostIds() {
        Hashtag hashtag = Hashtag.builder().hashtagId(4L).tag("java").postCount(3).lastUsedAt(Instant.now()).build();
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.of(hashtag));
        when(postHashtagRepository.findByHashtagId(4L)).thenReturn(List.of(
                PostHashtag.builder().id(1L).postId(13L).hashtagId(4L).build(),
                PostHashtag.builder().id(2L).postId(15L).hashtagId(4L).build()
        ));

        assertEquals(List.of(13L, 15L), service.getPostsByHashtag("JAVA"));
    }

    @Test
    void searchHashtagsMapsRepositoryResults() {
        Hashtag hashtag = Hashtag.builder().hashtagId(4L).tag("java").postCount(3).lastUsedAt(Instant.now()).build();
        when(hashtagRepository.findByTagContainingIgnoreCase("ja")).thenReturn(List.of(hashtag));

        List<HashtagResponse> results = service.searchHashtags("ja");

        assertEquals(1, results.size());
        assertEquals("java", results.get(0).tag());
    }

    @Test
    void getHashtagCountDelegatesToRepository() {
        when(hashtagRepository.count()).thenReturn(11L);

        assertEquals(11L, service.getHashtagCount());
    }
}
