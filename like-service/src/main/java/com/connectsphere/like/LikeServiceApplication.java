package com.connectsphere.like;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Boots the Like Service application.
 */
@EnableCaching
@SpringBootApplication
@EnableFeignClients
public class LikeServiceApplication {
/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(LikeServiceApplication.class, args);
    }
}
