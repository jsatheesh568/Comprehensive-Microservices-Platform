package com.orders.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RABBITMQ CONFIGURATION - Order Service (Producer)
 * 
 * Configures RabbitMQ for sending messages to Notification Service.
 * This is the producer side of async communication.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * ORDER EXCHANGE
     * 
     * Topic exchange for routing order-related messages.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }

    /**
     * MESSAGE CONVERTER
     * 
     * Converts Java objects to JSON for RabbitMQ messages.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RABBIT TEMPLATE
     * 
     * Template for sending messages to RabbitMQ.
     * Used by OrderService to send notifications.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
