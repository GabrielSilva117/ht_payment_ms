package io.gabrielsilva117.ht_payment_ms.publishers;

import io.gabrielsilva117.ht_payment_ms.config.MQConfig;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentEvent(String type, PaymentDTO paymentDTO) {
        String routingKey = String.format("payment.%s", type);

        rabbitTemplate.convertAndSend(
                MQConfig.TOPIC_EXCHANGE,
                routingKey,
                paymentDTO
        );
    }
}
