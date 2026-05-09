package com.connectsphere.post.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Declares the remote contract for Auth Service integration.
 */
@FeignClient(name = "auth-service", url = "${clients.auth-service.url:http://localhost:8080}")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    UserSummary getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/auth/search")
    List<UserSummary> searchUsers(@RequestParam("query") String query);
}
