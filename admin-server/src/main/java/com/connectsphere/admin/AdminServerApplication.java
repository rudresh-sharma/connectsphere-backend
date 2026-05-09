package com.connectsphere.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boots the Admin Server application.
 */
@SpringBootApplication
@EnableAdminServer
public class AdminServerApplication {
/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
