package com.connectsphere.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configures Cloudinary infrastructure for the service.
 */
@Component
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private String folder = "connectsphere/profiles";

/**
 * Returns cloud name.
 * @return operation result
 */
    public String getCloudName() {
        return cloudName;
    }

/**
 * Sets cloud name.
 * @param cloudName method input parameter
 */
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

/**
 * Returns api key.
 * @return operation result
 */
    public String getApiKey() {
        return apiKey;
    }

/**
 * Sets api key.
 * @param apiKey method input parameter
 */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

/**
 * Returns api secret.
 * @return operation result
 */
    public String getApiSecret() {
        return apiSecret;
    }

/**
 * Sets api secret.
 * @param apiSecret method input parameter
 */
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

/**
 * Returns folder.
 * @return operation result
 */
    public String getFolder() {
        return folder;
    }

/**
 * Sets folder.
 * @param folder method input parameter
 */
    public void setFolder(String folder) {
        this.folder = folder;
    }
}
