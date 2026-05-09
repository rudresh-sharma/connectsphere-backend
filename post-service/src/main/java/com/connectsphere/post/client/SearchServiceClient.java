package com.connectsphere.post.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Declares the remote contract for Search Service integration.
 */
@FeignClient(name = "search-service", url = "${clients.search-service.url:http://localhost:8087}")
public interface SearchServiceClient {

    @PostMapping("/search/index")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void indexPost(@RequestBody IndexPostRequest request);

    @DeleteMapping("/search/index/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removePostIndex(@PathVariable("postId") Long postId);
}
