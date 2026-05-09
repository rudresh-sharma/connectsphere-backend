package com.connectsphere.follow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Boots the Follow Service application.
 */
@EnableCaching
@EnableFeignClients
@SpringBootApplication
public class FollowServiceApplication {

/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(FollowServiceApplication.class, args);
    }
}
