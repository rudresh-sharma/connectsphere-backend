package com.connectsphere.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Boots the Service Registry application.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {
/**
 * Starts the Spring Boot application.
 * @param args application startup arguments
 */
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
