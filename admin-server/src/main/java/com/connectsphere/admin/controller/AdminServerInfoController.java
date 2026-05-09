package com.connectsphere.admin.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Exposes Admin Server Info API endpoints.
 */


@RestController
@RequestMapping("/admin")

public class AdminServerInfoController {
/**
 * Handles the info request.
 * @return resulting value
 */

    @GetMapping("/info")
    public AdminServerInfoResponse info() {
        return new AdminServerInfoResponse(
                "admin-server",
                8090,
                "/applications",
                List.of(
                        "eureka-server",
                        "api-gateway",
                        "auth-service",
                        "post-service",
                        "follow-service",
                        "comment-service",
                        "like-service",
                        "notification-service",
                        "media-service",
                        "search-service"
                )
        );
    }

    public record AdminServerInfoResponse(
            String serviceName,
            int port,
            String dashboardPath,
            List<String> expectedServices
    ) {
    }
}
