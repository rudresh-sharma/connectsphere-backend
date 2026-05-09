package com.connectsphere.search.service;

import com.connectsphere.search.dto.HashtagResponse;
import com.connectsphere.search.dto.IndexPostRequest;
import com.connectsphere.search.dto.PostSearchResult;
import java.util.List;

/**
 * Defines Search business operations.
 */
public interface SearchService {
    void indexPost(IndexPostRequest request);
    void removePostIndex(Long postId);
    List<PostSearchResult> searchPosts(String keyword);
    List<HashtagResponse> getHashtagsForPost(Long postId);
    List<HashtagResponse> getTrendingHashtags(int limit);
    List<Long> getPostsByHashtag(String tag);
    List<HashtagResponse> searchHashtags(String keyword);
    long getHashtagCount();
}
