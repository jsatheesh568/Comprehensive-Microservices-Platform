package com.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RABBITMQ CONFIGURATION
 * 
 * Configures RabbitMQ exchanges, queues, and bindings.
 * 
 * @Configuration - Marks this class as a Spring configuration class.
 *   Beans defined here are created when application starts.
 * 
 * RABBITMQ ARCHITECTURE:
 * 
 * 1. EXCHANGE:
 *    - Receives messages from producers
 *    - Routes messages to queues based on routing key
 *    - Types:
 *      * Direct: Routes to queue with exact routing key match
 *      * Topic: Routes based on pattern matching
 *      * Fanout: Broadcasts to all bound queues
 *      * Headers: Routes based on message headers
 * 
 * 2. QUEUE:
 *    - Stores messages
 *    - Consumers read from queues
 *    - Can be durable (survives broker restart)
 * 
 * 3. BINDING:
 *    - Connects exchange to queue
 *    - Defines routing key pattern
 * 
 * MESSAGE FLOW:
 * Producer -> Exchange -> Binding -> Queue -> Consumer
 */
@Configuration
public class RabbitMQConfig {

    /**
     * ORDER EXCHANGE
     * 
     * Topic exchange for order-related messages.
     * Topic exchanges route messages based on routing key patterns.
     * 
     * @Bean - Creates a Spring bean (Exchange object).
     *   This bean is managed by Spring and can be injected.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }

    /**
     * ORDER CREATED QUEUE
     * 
     * Queue for order created notifications.
     * 
     * QueueBuilder.durable() - Creates a durable queue.
     *   Durable queues survive broker restart.
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable("order.created.queue").build();
    }

    /**
     * BINDING
     * 
     * Binds the queue to the exchange with routing key "order.created".
     * 
     * When a message is sent to "order.exchange" with routing key "order.created",
     * it will be routed to "order.created.queue".
     */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(orderExchange())
                .with("order.created");
    }

    /**
     * MESSAGE CONVERTER
     * 
     * Converts Java objects to JSON and vice versa.
     * This allows sending/receiving Java objects as JSON messages.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RABBIT TEMPLATE
     * 
     * Template for sending messages to RabbitMQ.
     * Configured with JSON message converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
