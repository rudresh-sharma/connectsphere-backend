package com.connectsphere.follow.client;

import com.connectsphere.follow.messaging.RabbitMqConfig;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Declares the remote contract for Notification integration.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificationClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendFollowNotification(Long recipientId, Long actorId, String actorUsername) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("recipientId", recipientId);
            event.put("actorId", actorId);
            event.put("type", "FOLLOW");
            event.put("message", actorUsername + " started following you");
            event.put("targetId", null);
            event.put("targetType", null);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.NOTIFICATIONS_EXCHANGE,
                    RabbitMqConfig.NOTIFICATION_CREATED_ROUTING_KEY,
                    event
            );
            log.info("Follow notification sent for actorId={} to recipientId={}", actorId, recipientId);
        } catch (Exception ex) {
            log.warn("Failed to send follow notification for actorId={} to recipientId={}: {}",
                    actorId, recipientId, ex.getMessage());
        }
    }
}