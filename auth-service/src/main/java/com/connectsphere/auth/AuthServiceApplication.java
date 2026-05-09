package com.connectsphere.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Boots the Auth Service application.
 */
@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
public class AuthServiceApplication {

/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
