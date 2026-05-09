package com.connectsphere.post.exception;

/**
 * Signals a Forbidden error scenario.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
