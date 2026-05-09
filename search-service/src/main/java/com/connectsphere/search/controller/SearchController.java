package com.connectsphere.search.controller;

import com.connectsphere.search.dto.HashtagResponse;
import com.connectsphere.search.dto.IndexPostRequest;
import com.connectsphere.search.dto.PostSearchResult;
import com.connectsphere.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * Exposes Search API endpoints.
 */


@RestController
@RequiredArgsConstructor

public class SearchController {

    private final SearchService searchService;

    // ---- Indexing ----
/**
 * Handles the post request.
 * @param request request payload
 */
    @PostMapping("/search/index")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void indexPost(@Valid @RequestBody IndexPostRequest request) {
        searchService.indexPost(request);
    }
/**
 * Handles the post index request.
 * @param postId entity identifier
 */

    @DeleteMapping("/search/index/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePostIndex(@PathVariable("postId") Long postId) {
        searchService.removePostIndex(postId);
    }

    // ---- Search ----
/**
 * Searches for posts.
 * @param keyword search term
 * @return matching results
 */
    @GetMapping("/search/posts")
    public List<PostSearchResult> searchPosts(@RequestParam("q") String keyword) {
        return searchService.searchPosts(keyword);
    }

    // ---- Hashtags ----
/**
 * Returns trending hashtags.
 * @param "limit" method input parameter
 * @param limit method input parameter
 * @return matching results
 */
    @GetMapping("/hashtags/trending")
    public List<HashtagResponse> getTrendingHashtags(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return searchService.getTrendingHashtags(limit);
    }
/**
 * Returns posts by hashtag.
 * @param tag method input parameter
 * @return matching results
 */

    @GetMapping("/hashtags/{tag}/posts")
    public List<Long> getPostsByHashtag(@PathVariable("tag") String tag) {
        return searchService.getPostsByHashtag(tag);
    }
/**
 * Searches for hashtags.
 * @param keyword search term
 * @return matching results
 */

    @GetMapping("/hashtags/search")
    public List<HashtagResponse> searchHashtags(@RequestParam("q") String keyword) {
        return searchService.searchHashtags(keyword);
    }
/**
 * Returns hashtags for post.
 * @param postId entity identifier
 * @return matching results
 */

    @GetMapping("/hashtags/post/{postId}")
    public List<HashtagResponse> getHashtagsForPost(@PathVariable("postId") Long postId) {
        return searchService.getHashtagsForPost(postId);
    }
/**
 * Returns hashtag count.
 * @return operation result
 */

    @GetMapping("/hashtags/count")
    public long getHashtagCount() {
        return searchService.getHashtagCount();
    }
}
