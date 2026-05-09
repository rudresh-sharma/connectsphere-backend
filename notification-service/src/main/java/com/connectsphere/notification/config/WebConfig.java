package com.connectsphere.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Web infrastructure for the service.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origin:http://localhost:4200}")
    private String allowedOrigin;
/**
 * Adds cors mappings.
 * @param registry method input parameter
 */

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin, "http://127.0.0.1:4200")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
