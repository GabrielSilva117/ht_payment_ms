package io.gabrielsilva117.ht_payment_ms.consumers;

import io.gabrielsilva117.ht_payment_ms.config.MQConfig;
import io.gabrielsilva117.ht_payment_ms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConsumers {
    private final PaymentService paymentService;

    @RabbitListener(queues = MQConfig.PAYMENT_QUEUE)
    public void handlePayments(String message) {
        try {
            this.paymentService.processPayment(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
