package com.connectsphere.post.exception;

/**
 * Signals a Bad Request error scenario.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
