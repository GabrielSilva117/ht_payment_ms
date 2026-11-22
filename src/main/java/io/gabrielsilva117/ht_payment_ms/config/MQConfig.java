package io.gabrielsilva117.ht_payment_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {
    public static final String TOPIC_EXCHANGE = "payment.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    public static final String PAYMENT_QUEUE = "payment_dispatch";
    public static final String NOTIFICATION_QUEUE = "email_dispatch";

    // Scoped to outbound publish only (RabbitTemplate) — deliberately not a global
    // MessageConverter bean, which would also get picked up by the @RabbitListener
    // container factory and break PaymentConsumers' raw String payload handling.
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public DirectExchange notificationExchange() { return new DirectExchange(NOTIFICATION_EXCHANGE); }

    // Queue declarations
    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    // No bindings here: payment_dispatch carries only inbound processing requests
    // (delivered by the monolith direct-to-queue). It previously also bound to
    // "payment.*.created"/"payment.card.*" on paymentExchange — copy-pasted from an
    // order-service template since the initial commit — which looped this service's
    // own outbound PaymentPublisher lifecycle events (card.created/card.failed) back
    // into PaymentConsumers as if they were new payment requests. No consumer in this
    // codebase currently listens for those lifecycle events; a future one should bind
    // its own queue to paymentExchange rather than reuse payment_dispatch.

//    @Bean
//    public Binding debitCardBinding(Queue paymentQueue, TopicExchange paymentExchange) {
//        return BindingBuilder.bind(paymentQueue)
//                .to(paymentExchange)
//                .with("payment.card.debit.*");
//    }
//    @Bean
//    public Binding notificationBinding(Queue notificationQueue, TopicExchange orderExchange) {
//        // Listen to all order events (created, shipped, delivered, etc.)
//        return BindingBuilder.bind(notificationQueue)
//                .to(orderExchange)
//                .with("order.#");
//    }


}
