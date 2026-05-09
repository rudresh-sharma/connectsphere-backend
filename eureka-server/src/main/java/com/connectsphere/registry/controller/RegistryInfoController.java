package com.connectsphere.registry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Exposes Registry Info API endpoints.
 */


@RestController
@RequestMapping("/registry")
@Tag(name = "Service Registry", description = "Convenience endpoints around the Eureka registry.")

public class RegistryInfoController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private int serverPort;

    @GetMapping("/info")
    @Operation(
            summary = "Get registry metadata",
            description = "Returns basic metadata and useful URLs for the Eureka service registry."
    )
/**
 * Handles the info request.
 * @return resulting value
 */
    public RegistryInfoResponse info() {
        return new RegistryInfoResponse(
                applicationName,
                serverPort,
                "/",
                "/swagger-ui.html",
                "/v3/api-docs",
                "/actuator/health"
        );
    }

    public record RegistryInfoResponse(
            String serviceName,
            int port,
            String dashboardPath,
            String swaggerUiPath,
            String openApiDocsPath,
            String healthPath
    ) {
    }
}
