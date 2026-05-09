package com.connectsphere.auth.client;

import com.connectsphere.auth.messaging.RabbitMqConfig;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Declares the remote contract for Notification integration.
 */
@Component
public class NotificationClient {

    private final RabbitTemplate rabbitTemplate;

    public NotificationClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendWelcomeEmail(Long recipientId, String email, String username, String fullName) {
        Map<String, Object> event = new HashMap<>();
        event.put("recipientId", recipientId);
        event.put("email", email);
        event.put("username", username);
        event.put("fullName", fullName);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATIONS_EXCHANGE,
                RabbitMqConfig.WELCOME_EMAIL_ROUTING_KEY,
                event
        );
    }
}
