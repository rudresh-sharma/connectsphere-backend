package com.connectsphere.auth.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the payload used for API Error operations.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}

