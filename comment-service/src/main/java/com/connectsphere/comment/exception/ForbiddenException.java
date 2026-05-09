package com.connectsphere.comment.exception;

/**
 * Signals a Forbidden error scenario.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
