package com.connectsphere.notification.exception;

/**
 * Signals a Resource Not Found error scenario.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}


