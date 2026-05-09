package com.connectsphere.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the payload used for Welcome Email operations.
 */
public record WelcomeEmailRequest(
        @NotNull Long recipientId,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 40) String username,
        @NotBlank @Size(max = 120) String fullName
) {
}
