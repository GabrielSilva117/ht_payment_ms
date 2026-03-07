package io.gabrielsilva117.ht_payment_ms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import io.gabrielsilva117.ht_payment_ms.dto.NotificationDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import io.gabrielsilva117.ht_payment_ms.processor.PaymentFactory;
import io.gabrielsilva117.ht_payment_ms.processor.PaymentProcessor;
import io.gabrielsilva117.ht_payment_ms.publishers.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final ObjectMapper objectMapper;
    private final NotificationPublisher notificationPublisher;
    private final PaymentFactory paymentFactory;

    public PaymentDTO constructDTO (String rawObj) throws JsonProcessingException {
        return this.objectMapper.readValue(rawObj, PaymentDTO.class);
    }

    public void processPayment(String payload, Channel channel, Message msg) throws IOException {
        triggerPayment(this.constructDTO(payload), msg, channel);

        notificationPublisher.publishNotification(new NotificationDTO(
                "Purchase Complete!",
                "Congratulations, your purchase has been completed!",
                "gabriel.f.silva117@gmail.com"
        ));
    }

    private void triggerPayment(PaymentDTO paymentDTO, Message msg, Channel channel) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();

        try {
            PaymentProcessor processor = paymentFactory.getProcessor(paymentDTO.getType());
            // run common validations

            processor.process(paymentDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            handleError(paymentDTO, msg, channel, deliveryTag, exception);
        }
    }

    private void handleError(PaymentDTO payment, Message message, Channel channel,
                             long deliveryTag, Exception e) throws IOException {
        int retryCount = getRetryCount(message);
        if (retryCount < 3) {
            logger.warn("Requeuing payment {} (retry {})", payment.getCartId(), retryCount);
            channel.basicNack(deliveryTag, false, true);
        } else {
            logger.error("Max retries reached for payment {}. Sending to DLQ",
                    payment.getCartId());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private int getRetryCount(Message message) {
        Integer count = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
        return count != null ? count : 0;
    }
}
