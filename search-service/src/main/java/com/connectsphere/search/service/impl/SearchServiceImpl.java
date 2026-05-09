package com.connectsphere.search.service.impl;

import com.connectsphere.search.config.CacheConfig;
import com.connectsphere.search.dto.HashtagResponse;
import com.connectsphere.search.dto.IndexPostRequest;
import com.connectsphere.search.dto.PostSearchResult;
import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostDocument;
import com.connectsphere.search.entity.PostHashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.PostHashtagRepository;
import com.connectsphere.search.repository.PostSearchRepository;
import com.connectsphere.search.service.SearchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Implements Search business operations.
 */


@Service
@RequiredArgsConstructor
@Transactional

public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostSearchRepository postSearchRepository;
    private final CacheManager cacheManager;
/**
 * Indexes post.
 * @param request request payload
 */

    @Override
    public void indexPost(IndexPostRequest request) {
        // Index in Elasticsearch
        PostDocument doc = PostDocument.builder()
                .id(String.valueOf(request.postId()))
                .postId(request.postId())
                .authorId(request.authorId())
                .content(request.content())
                .authorUsername(request.authorUsername())
                .visibility(request.visibility())
                .build();

        try {
            postSearchRepository.save(doc);
        } catch (Exception ex) {
            log.warn("Could not index post {} in Elasticsearch: {}", request.postId(), ex.getMessage());
        }

        // Extract and persist hashtags
        if (request.content() != null) {
            List<String> tags = extractHashtags(request.content());
            for (String tag : tags) {
                Hashtag hashtag = hashtagRepository.findByTag(tag)
                        .orElseGet(() -> hashtagRepository.save(Hashtag.builder().tag(tag).postCount(0).lastUsedAt(Instant.now()).build()));

                if (!postHashtagRepository.existsByPostIdAndHashtagId(request.postId(), hashtag.getHashtagId())) {
                    postHashtagRepository.save(PostHashtag.builder().postId(request.postId()).hashtagId(hashtag.getHashtagId()).build());
                    hashtag.setPostCount(hashtag.getPostCount() + 1);
                    hashtag.setLastUsedAt(Instant.now());
                    hashtagRepository.save(hashtag);
                }
            }
        }
        clearSearchCaches();
    }
/**
 * Removes post index.
 * @param postId entity identifier
 */

    @Override
    public void removePostIndex(Long postId) {
        try {
            postSearchRepository.deleteById(String.valueOf(postId));
        } catch (Exception ex) {
            log.warn("Could not remove post {} from Elasticsearch: {}", postId, ex.getMessage());
        }

        List<PostHashtag> mappings = postHashtagRepository.findByPostId(postId);
        for (PostHashtag mapping : mappings) {
            hashtagRepository.findById(mapping.getHashtagId()).ifPresent(h -> {
                h.setPostCount(Math.max(0, h.getPostCount() - 1));
                hashtagRepository.save(h);
            });
        }
        postHashtagRepository.deleteByPostId(postId);
        clearSearchCaches();
    }
/**
 * Searches posts.
 * @param keyword search term
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.POST_SEARCHES_CACHE, key = "#p0 == null ? '' : #p0.toLowerCase()")
    public List<PostSearchResult> searchPosts(String keyword) {
        try {
            return postSearchRepository.findByContentContaining(keyword)
                    .stream()
                    .map(d -> new PostSearchResult(d.getPostId(), d.getAuthorId(), d.getContent(), d.getAuthorUsername(), d.getVisibility()))
                    .toList();
        } catch (Exception ex) {
            log.warn("Elasticsearch search failed, returning empty: {}", ex.getMessage());
            return List.of();
        }
    }
/**
 * Returns hashtags for post.
 * @param postId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.POST_HASHTAGS_CACHE, key = "#p0")
    public List<HashtagResponse> getHashtagsForPost(Long postId) {
        List<PostHashtag> mappings = postHashtagRepository.findByPostId(postId);
        List<HashtagResponse> result = new ArrayList<>();
        for (PostHashtag m : mappings) {
            hashtagRepository.findById(m.getHashtagId()).ifPresent(h -> result.add(toResponse(h)));
        }
        return result;
    }
/**
 * Returns trending hashtags.
 * @param limit method input parameter
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.TRENDING_HASHTAGS_CACHE, key = "#p0")
    public List<HashtagResponse> getTrendingHashtags(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return hashtagRepository.findAllByOrderByPostCountDescLastUsedAtDesc(PageRequest.of(0, safeLimit))
                .stream().map(this::toResponse).toList();
    }
/**
 * Returns posts by hashtag.
 * @param tag method input parameter
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.HASHTAG_POSTS_CACHE, key = "#p0 == null ? '' : #p0.toLowerCase()")
    public List<Long> getPostsByHashtag(String tag) {
        return hashtagRepository.findByTag(tag.toLowerCase())
                .map(h -> postHashtagRepository.findByHashtagId(h.getHashtagId())
                        .stream().map(PostHashtag::getPostId).toList())
                .orElse(List.of());
    }
/**
 * Searches hashtags.
 * @param keyword search term
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.HASHTAG_SEARCHES_CACHE, key = "#p0 == null ? '' : #p0.toLowerCase()")
    public List<HashtagResponse> searchHashtags(String keyword) {
        return hashtagRepository.findByTagContainingIgnoreCase(keyword)
                .stream().map(this::toResponse).toList();
    }
/**
 * Returns hashtag count.
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.HASHTAG_COUNT_CACHE, key = "'all'")
    public long getHashtagCount() {
        return hashtagRepository.count();
    }

    private List<String> extractHashtags(String content) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase());
        }
        return tags;
    }

    private HashtagResponse toResponse(Hashtag h) {
        return new HashtagResponse(h.getHashtagId(), h.getTag(), h.getPostCount(), h.getLastUsedAt());
    }

    private void clearSearchCaches() {
        clear(CacheConfig.POST_SEARCHES_CACHE);
        clear(CacheConfig.POST_HASHTAGS_CACHE);
        clear(CacheConfig.TRENDING_HASHTAGS_CACHE);
        clear(CacheConfig.HASHTAG_POSTS_CACHE);
        clear(CacheConfig.HASHTAG_SEARCHES_CACHE);
        clear(CacheConfig.HASHTAG_COUNT_CACHE);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
