package com.connectsphere.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declares the remote contract for Post Service integration.
 */
@FeignClient(name = "post-service", url = "${clients.post-service.url:http://localhost:8081}")
public interface PostServiceClient {

    @PatchMapping("/posts/{postId}/count")
    Object updateCounter(@PathVariable("postId") Long postId, @RequestBody CounterRequest request);

    record CounterRequest(String counterType, long delta) {
    }
}
