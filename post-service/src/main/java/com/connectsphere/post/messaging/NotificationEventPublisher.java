package com.connectsphere.post.messaging;

import com.connectsphere.post.client.CreateNotificationRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Supports Notification Event Publisher messaging workflows.
 */
@Component
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

/**
 * Performs the publish operation.
 * @param request request payload
 */
    public void publish(CreateNotificationRequest request) {
        Map<String, Object> event = new HashMap<>();
        event.put("recipientId", request.recipientId());
        event.put("actorId", request.actorId());
        event.put("type", request.type().name());
        event.put("message", request.message());
        event.put("targetId", request.targetId());
        event.put("targetType", request.targetType());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATIONS_EXCHANGE,
                RabbitMqConfig.NOTIFICATION_CREATED_ROUTING_KEY,
                event
        );
    }
}
