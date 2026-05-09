package com.connectsphere.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Exposes Gateway Info API endpoints.
 */


@RestController
@RequestMapping("/gateway")
@Tag(name = "API Gateway", description = "Gateway metadata and routed path overview.")

public class GatewayInfoController {
/**
 * Handles the routes request.
 * @return operation result
 */

    @GetMapping("/routes")
    @Operation(summary = "List routed path prefixes", description = "Returns the public path prefixes exposed by the API gateway.")
    public GatewayRoutesResponse routes() {
        return new GatewayRoutesResponse(
                "api-gateway",
                8088,
                List.of(
                        new GatewayRoute("auth-service", List.of("/auth/**", "/oauth2/**", "/login/oauth2/**")),
                        new GatewayRoute("post-service", List.of("/posts/**")),
                        new GatewayRoute("follow-service", List.of("/follows/**")),
                        new GatewayRoute("comment-service", List.of("/comments/**")),
                        new GatewayRoute("like-service", List.of("/likes/**")),
                        new GatewayRoute("notification-service", List.of("/notifications/**")),
                        new GatewayRoute("media-service", List.of("/media/**", "/stories/**", "/reels/**")),
                        new GatewayRoute("search-service", List.of("/search/**", "/hashtags/**"))
                )
        );
    }

    public record GatewayRoutesResponse(String serviceName, int port, List<GatewayRoute> routes) {
    }

    public record GatewayRoute(String targetService, List<String> pathPatterns) {
    }
}
