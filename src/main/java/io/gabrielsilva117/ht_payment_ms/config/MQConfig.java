package io.gabrielsilva117.ht_payment_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {
    public static final String TOPIC_EXCHANGE = "payment.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    public static final String PAYMENT_QUEUE = "payment_dispatch";
    public static final String NOTIFICATION_QUEUE = "email_dispatch";

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

    // Bindings
    @Bean
    public Binding successPaymentBinding(Queue paymentQueue, TopicExchange paymentExchange) {
        // Listen to all "created" orders regardless of category
        return BindingBuilder.bind(paymentQueue)
                .to(paymentExchange)
                .with("payment.*.created");
    }


    @Bean
    public Binding creditCardBinding(Queue paymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(paymentExchange)
                .with("payment.card.credit.*");
    }

    @Bean
    public Binding debitCardBinding(Queue paymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(paymentExchange)
                .with("payment.card.debit.*");
    }
//    @Bean
//    public Binding notificationBinding(Queue notificationQueue, TopicExchange orderExchange) {
//        // Listen to all order events (created, shipped, delivered, etc.)
//        return BindingBuilder.bind(notificationQueue)
//                .to(orderExchange)
//                .with("order.#");
//    }


}
