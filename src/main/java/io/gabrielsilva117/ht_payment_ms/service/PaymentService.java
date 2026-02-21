package io.gabrielsilva117.ht_payment_ms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gabrielsilva117.ht_payment_ms.dto.NotificationDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import io.gabrielsilva117.ht_payment_ms.processor.PaymentFactory;
import io.gabrielsilva117.ht_payment_ms.publishers.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final ObjectMapper objectMapper;
    private final NotificationPublisher notificationPublisher;
    private final PaymentFactory paymentFactory;

    public PaymentDTO constructDTO (String rawObj) throws JsonProcessingException {
        return this.objectMapper.readValue(rawObj, PaymentDTO.class);
    }

    public void processPayment(String message) throws JsonProcessingException {

        notificationPublisher.publishNotification(new NotificationDTO(
                "Purchase Complete!",
                "Congratulations, your purchase has been completed!",
                "gabriel.f.silva117@gmail.com"
        ));
    }
}
