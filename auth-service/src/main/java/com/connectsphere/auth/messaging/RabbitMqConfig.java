package com.connectsphere.auth.messaging;

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
    public static final String WELCOME_EMAIL_QUEUE = "connectsphere.notification.email.welcome";
    public static final String WELCOME_EMAIL_ROUTING_KEY = "notification.email.welcome";
/**
 * Performs the notifications exchange operation.
 * @return resulting value
 */

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }
/**
 * Performs the welcome email queue operation.
 * @return resulting value
 */

    @Bean
    public Queue welcomeEmailQueue() {
        return new Queue(WELCOME_EMAIL_QUEUE, true);
    }
/**
 * Performs the welcome email binding operation.
 * @param welcomeEmailQueue method input parameter
 * @param notificationsExchange method input parameter
 * @return resulting value
 */

    @Bean
    public Binding welcomeEmailBinding(Queue welcomeEmailQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(welcomeEmailQueue)
                .to(notificationsExchange)
                .with(WELCOME_EMAIL_ROUTING_KEY);
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
