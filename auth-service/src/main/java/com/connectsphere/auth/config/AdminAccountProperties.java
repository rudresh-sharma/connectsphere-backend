package com.connectsphere.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configures Admin Account infrastructure for the service.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.admin")
public class AdminAccountProperties {

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

/**
 * Returns username.
 * @return operation result
 */
    public String getUsername() {
        return username;
    }

/**
 * Sets username.
 * @param username method input parameter
 */
    public void setUsername(String username) {
        this.username = username;
    }

/**
 * Returns email.
 * @return operation result
 */
    public String getEmail() {
        return email;
    }

/**
 * Sets email.
 * @param email method input parameter
 */
    public void setEmail(String email) {
        this.email = email;
    }

/**
 * Returns password.
 * @return operation result
 */
    public String getPassword() {
        return password;
    }

/**
 * Sets password.
 * @param password method input parameter
 */
    public void setPassword(String password) {
        this.password = password;
    }

/**
 * Returns full name.
 * @return operation result
 */
    public String getFullName() {
        return fullName;
    }

/**
 * Sets full name.
 * @param fullName method input parameter
 */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
