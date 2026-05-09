package com.connectsphere.follow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declares the remote contract for Auth Service integration.
 */
@FeignClient(name = "auth-service", url = "${clients.auth-service.url:http://localhost:8080}")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    UserSummary getUserById(@PathVariable("userId") Long userId);
}