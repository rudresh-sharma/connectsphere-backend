package com.connectsphere.post.config;

import com.cloudinary.Cloudinary;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configures Cloudinary infrastructure for the service.
 */
@Configuration
public class CloudinaryConfig {
/**
 * Performs the cloudinary operation.
 * @param properties method input parameter
 * @return resulting value
 */

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        if (!StringUtils.hasText(properties.getCloudName())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getApiSecret())) {
            throw new IllegalStateException("Cloudinary credentials are missing");
        }

        return new Cloudinary(Map.of(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true
        ));
    }
}
