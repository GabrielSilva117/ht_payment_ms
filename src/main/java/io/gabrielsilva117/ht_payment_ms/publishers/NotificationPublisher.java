package io.gabrielsilva117.ht_payment_ms.publishers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gabrielsilva117.ht_payment_ms.config.MQConfig;
import io.gabrielsilva117.ht_payment_ms.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishNotification(NotificationDTO notificationDTO) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(notificationDTO);

        rabbitTemplate.convertAndSend(
                MQConfig.NOTIFICATION_EXCHANGE,
                "email.create",
                json
        );
    }
}
