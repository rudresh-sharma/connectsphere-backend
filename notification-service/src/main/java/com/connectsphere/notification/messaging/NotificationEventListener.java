package com.connectsphere.notification.messaging;

import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.dto.WelcomeEmailRequest;
import com.connectsphere.notification.entity.NotificationType;
import com.connectsphere.notification.service.NotificationService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Supports Notification Event Listener messaging workflows.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
/**
 * Handles notification created.
 * @param Map<String method input parameter
 * @param event method input parameter
 */

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_CREATED_QUEUE)
    public void handleNotificationCreated(Map<String, Object> event) {
        log.info("Received notification event: {}", event);
        try {
            notificationService.createNotification(new CreateNotificationRequest(
                    asLong(event.get("recipientId")),
                    asLong(event.get("actorId")),
                    NotificationType.valueOf(String.valueOf(event.get("type"))),
                    asString(event.get("message")),
                    asLong(event.get("targetId")),
                    asString(event.get("targetType"))
            ));
            log.info("Notification created successfully for type: {}", event.get("type"));
        } catch (Exception ex) {
            log.error("Error creating notification from event: {}", event, ex);
        }
    }
/**
 * Handles welcome email.
 * @param Map<String method input parameter
 * @param event method input parameter
 */

    @RabbitListener(queues = RabbitMqConfig.WELCOME_EMAIL_QUEUE)
    public void handleWelcomeEmail(Map<String, Object> event) {
        notificationService.sendWelcomeEmail(new WelcomeEmailRequest(
                asLong(event.get("recipientId")),
                asString(event.get("email")),
                asString(event.get("username")),
                asString(event.get("fullName"))
        ));
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
