package io.gabrielsilva117.ht_payment_ms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import io.gabrielsilva117.ht_payment_ms.dto.NotificationDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentResponse;
import io.gabrielsilva117.ht_payment_ms.entity.Payment;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentEntityStatus;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentStatus;
import io.gabrielsilva117.ht_payment_ms.exception.DuplicatePaymentException;
import io.gabrielsilva117.ht_payment_ms.exception.PaymentPersistenceException;
import io.gabrielsilva117.ht_payment_ms.processor.PaymentFactory;
import io.gabrielsilva117.ht_payment_ms.processor.PaymentProcessor;
import io.gabrielsilva117.ht_payment_ms.processor.PaymentProcessorResult;
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
    private final PaymentPersistenceService paymentPersistenceService;

    public PaymentDTO constructDTO(String rawObj) throws JsonProcessingException {
        return this.objectMapper.readValue(rawObj, PaymentDTO.class);
    }

    public PaymentResponse processPayment(PaymentDTO obj) {
        Payment payment;
        try {
            payment = paymentPersistenceService.createPending(obj);
        } catch (DuplicatePaymentException dup) {
            logger.info("Duplicate payment request for cartId {}", obj.getCartId());
            Payment existing = paymentPersistenceService.findByCartId(obj.getCartId());
            return new PaymentResponse(toApiStatus(existing.getStatus()));
        }

        return new PaymentResponse(runGateway(payment, obj));
    }

    public void processPayment(String payload, Channel channel, Message msg) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();

        PaymentDTO dto;
        try {
            dto = constructDTO(payload);
        } catch (JsonProcessingException e) {
            logger.error("Malformed payment payload, routing to DLQ", e);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        Payment payment;
        try {
            payment = paymentPersistenceService.createPending(dto);
        } catch (DuplicatePaymentException dup) {
            logger.warn("Duplicate delivery for cartId {}, acking without reprocessing", dto.getCartId());
            channel.basicAck(deliveryTag, false);
            return;
        } catch (PaymentPersistenceException persistenceFailure) {
            logger.error("Failed to persist payment {}, requeueing", dto.getCartId(), persistenceFailure);
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        // Row is durably committed -> ack now, unconditionally, before the gateway call runs.
        channel.basicAck(deliveryTag, false);

        runGateway(payment, dto);
    }

    private PaymentStatus runGateway(Payment payment, PaymentDTO dto) {
        try {
            PaymentProcessor processor = paymentFactory.getProcessor(dto.getType());
            PaymentProcessorResult result = processor.process(dto);
            if (result.isPending()) {
                paymentPersistenceService.markProcessing(
                        payment.getId(), result.getGatewayReference(), result.getCardLast4());
                return PaymentStatus.PROCESSING;
            }
            if (result.isSuccess()) {
                paymentPersistenceService.markSucceeded(payment.getId(), result.getGatewayReference());
                notifySuccess();
                return PaymentStatus.SUCCESS;
            }
            paymentPersistenceService.markFailed(payment.getId(), result.getFailureReason());
            return PaymentStatus.FAILURE;
        } catch (Exception unexpected) {
            logger.error("Unexpected error invoking gateway for payment {}", dto.getCartId(), unexpected);
            paymentPersistenceService.markFailed(payment.getId(), unexpected.getMessage());
            return PaymentStatus.FAILURE;
        }
    }

    private void notifySuccess() {
        try {
            notificationPublisher.publishNotification(new NotificationDTO(
                    "Purchase Complete!",
                    "Congratulations, your purchase has been completed!",
                    "gabriel.f.silva117@gmail.com"
            ));
        } catch (JsonProcessingException e) {
            logger.error("Failed to publish success notification", e);
        }
    }

    private PaymentStatus toApiStatus(PaymentEntityStatus status) {
        return switch (status) {
            case SUCCEEDED -> PaymentStatus.SUCCESS;
            case PENDING, PROCESSING -> PaymentStatus.PROCESSING;
            case FAILED -> PaymentStatus.FAILURE;
        };
    }
}
