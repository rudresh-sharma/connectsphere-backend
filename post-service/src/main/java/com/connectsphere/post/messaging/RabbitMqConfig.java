package com.connectsphere.post.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supports RabbitMQ messaging workflows.
 */
@Configuration
public class RabbitMqConfig {

    public static final String NOTIFICATIONS_EXCHANGE = "connectsphere.notifications";
    public static final String NOTIFICATION_CREATED_QUEUE = "connectsphere.notification.created";
    public static final String NOTIFICATION_CREATED_ROUTING_KEY = "notification.created";
/**
 * Performs the notifications exchange operation.
 * @return resulting value
 */

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }
/**
 * Performs the notification created queue operation.
 * @return resulting value
 */

    @Bean
    public Queue notificationCreatedQueue() {
        return new Queue(NOTIFICATION_CREATED_QUEUE, true);
    }
/**
 * Performs the notification created binding operation.
 * @param notificationCreatedQueue method input parameter
 * @param notificationsExchange method input parameter
 * @return resulting value
 */

    @Bean
    public Binding notificationCreatedBinding(Queue notificationCreatedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(notificationCreatedQueue)
                .to(notificationsExchange)
                .with(NOTIFICATION_CREATED_ROUTING_KEY);
    }
/**
 * Performs the message converter operation.
 * @return resulting value
 */

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
