package io.gabrielsilva117.ht_payment_ms.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gabrielsilva117.ht_payment_ms.config.MQConfig;
import io.gabrielsilva117.ht_payment_ms.dto.NotificationDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.publishers.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConsumers {
    private final ObjectMapper objectMapper;
    private final NotificationPublisher notificationPublisher;

    @RabbitListener(queues = MQConfig.PAYMENT_QUEUE)
    public void handlePayments(String message) {
        try {
            PaymentDTO dto = this.objectMapper.readValue(message, PaymentDTO.class);
            System.out.println(dto.getAmount());

            notificationPublisher.publishNotification(new NotificationDTO(
                    "Purchase Complete!",
                    "Congratulations, your purchase has been completed!",
                    "gabriel.f.silva117@gmail.com"
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
