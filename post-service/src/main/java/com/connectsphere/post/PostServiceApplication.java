package com.connectsphere.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Boots the Post Service application.
 */
@EnableCaching
@EnableFeignClients
@SpringBootApplication
public class PostServiceApplication {

/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(PostServiceApplication.class, args);
    }
}
