package com.connectsphere.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds Cloudinary configuration values used by the media service.
 */
@Component
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private String folder = "connectsphere/media";

    /**
     * Returns the configured Cloudinary cloud name.
     *
     * @return Cloudinary cloud name
     */
    public String getCloudName() {
        return cloudName;
    }

    /**
     * Sets the Cloudinary cloud name.
     *
     * @param cloudName Cloudinary cloud name
     */
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    /**
     * Returns the configured Cloudinary API key.
     *
     * @return Cloudinary API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the Cloudinary API key.
     *
     * @param apiKey Cloudinary API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Returns the configured Cloudinary API secret.
     *
     * @return Cloudinary API secret
     */
    public String getApiSecret() {
        return apiSecret;
    }

    /**
     * Sets the Cloudinary API secret.
     *
     * @param apiSecret Cloudinary API secret
     */
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    /**
     * Returns the default Cloudinary folder used by the media service.
     *
     * @return Cloudinary folder path
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the default Cloudinary folder used by the media service.
     *
     * @param folder Cloudinary folder path
     */
    public void setFolder(String folder) {
        this.folder = folder;
    }
}
