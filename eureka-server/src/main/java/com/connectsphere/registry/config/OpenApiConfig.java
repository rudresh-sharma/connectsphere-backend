package com.connectsphere.registry.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI infrastructure for the service.
 */
@Configuration
public class OpenApiConfig {
/**
 * Performs the service registry OpenAPI operation.
 * @return resulting value
 */

    @Bean
    public OpenAPI serviceRegistryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Service Registry API")
                        .description("Swagger/OpenAPI support for the Eureka service registry and supporting registry endpoints.")
                        .version("v1")
                        .contact(new Contact().name("ConnectSphere"))
                        .license(new License().name("Internal Use")));
    }
}
