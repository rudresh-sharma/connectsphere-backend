package com.connectsphere.post.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.connectsphere.post.client.CreateNotificationRequest;
import com.connectsphere.post.client.NotificationType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Unit tests for NotificationEventPublisher.
 */
class NotificationEventPublisherTest {

    @Test
    void publishSendsMapToRabbitMq() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        NotificationEventPublisher publisher = new NotificationEventPublisher(rabbitTemplate);

        CreateNotificationRequest request = new CreateNotificationRequest(
                10L, 1L, NotificationType.MENTION, "You were mentioned", 5L, "POST"
        );

        publisher.publish(request);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.NOTIFICATIONS_EXCHANGE),
                eq(RabbitMqConfig.NOTIFICATION_CREATED_ROUTING_KEY),
                any(Map.class)
        );
    }
}
