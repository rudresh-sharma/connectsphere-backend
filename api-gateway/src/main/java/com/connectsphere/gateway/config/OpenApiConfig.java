package com.connectsphere.gateway.config;

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
 * Performs the api gateway OpenAPI operation.
 * @return resulting value
 */

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere API Gateway")
                        .description("Gateway metadata and route summary for the ConnectSphere microservices.")
                        .version("v1")
                        .contact(new Contact().name("ConnectSphere"))
                        .license(new License().name("Internal Use")));
    }
}
