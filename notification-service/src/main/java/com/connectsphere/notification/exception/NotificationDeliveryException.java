package com.connectsphere.notification.exception;

/**
 * Raised when the notification service cannot deliver an email message.
 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
