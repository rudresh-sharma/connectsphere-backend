package com.connectsphere.auth.config;

import com.cloudinary.Cloudinary;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Cloudinary infrastructure for the service.
 */
@Configuration
@ConditionalOnProperty(name = "cloudinary.cloud-name")
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);
/**
 * Performs the cloudinary operation.
 * @param properties method input parameter
 * @return resulting value
 */

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        log.info("Cloudinary configured successfully with cloud name: {}", properties.getCloudName());

        return new Cloudinary(Map.of(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true
        ));
    }
}
