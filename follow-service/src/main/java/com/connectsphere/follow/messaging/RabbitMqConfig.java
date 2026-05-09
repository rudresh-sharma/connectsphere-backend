package com.connectsphere.follow.messaging;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supports RabbitMQ messaging workflows.
 */
@Configuration
public class RabbitMqConfig {

    public static final String NOTIFICATIONS_EXCHANGE = "connectsphere.notifications";
    public static final String NOTIFICATION_CREATED_ROUTING_KEY = "notification.created";
/**
 * Performs the json message converter operation.
 * @return resulting value
 */

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
/**
 * Performs the rabbit template operation.
 * @param connectionFactory method input parameter
 * @return resulting value
 */

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}