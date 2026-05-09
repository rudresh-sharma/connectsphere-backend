package com.connectsphere.post.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declares the remote contract for Follow Service integration.
 */
@FeignClient(name = "follow-service", url = "${clients.follow-service.url:http://localhost:8082}")
public interface FollowServiceClient {

    @GetMapping("/follows/following-ids/{userId}")
    List<Long> getFollowingIds(@PathVariable("userId") Long userId);
}
