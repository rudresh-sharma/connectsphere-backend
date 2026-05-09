package com.connectsphere.auth.exception;

/**
 * Signals a Resource Not Found error scenario.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

