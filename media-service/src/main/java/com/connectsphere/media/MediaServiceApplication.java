package com.connectsphere.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Boots the Media Service application.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class MediaServiceApplication {
/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
