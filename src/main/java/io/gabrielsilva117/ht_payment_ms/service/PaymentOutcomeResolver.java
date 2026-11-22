package io.gabrielsilva117.ht_payment_ms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gabrielsilva117.ht_payment_ms.dto.CardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.NotificationDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.entity.Payment;
import io.gabrielsilva117.ht_payment_ms.publishers.NotificationPublisher;
import io.gabrielsilva117.ht_payment_ms.publishers.PaymentPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resolves a CARD payment to its terminal outcome, from whichever caller gets there first:
 * the Stripe webhook, a duplicate webhook redelivery, or the timeout sweep's reconciliation
 * against Stripe. Each resolve call is backed by an atomic conditional UPDATE
 * ({@code succeedIfStillLive}/{@code failIfStillLive}) - only the caller whose update
 * actually applies (the row was still PENDING/PROCESSING at that instant) fires the publish/
 * notification side effects; everyone else affected 0 rows and no-ops. There is no separate
 * "is this already terminal" check - the conditional UPDATE is that check, atomically.
 */
@Service
@RequiredArgsConstructor
public class PaymentOutcomeResolver {
    private static final Logger logger = LoggerFactory.getLogger(PaymentOutcomeResolver.class);

    private final PaymentPersistenceService paymentPersistenceService;
    private final PaymentPublisher paymentPublisher;
    private final NotificationPublisher notificationPublisher;

    public void resolveSucceeded(Payment payment, String gatewayReference) {
        boolean applied = paymentPersistenceService.succeedIfStillLive(payment.getId(), gatewayReference);
        if (!applied) {
            logger.debug("Payment {} already resolved, ignoring success resolution", payment.getId());
            return;
        }
        paymentPublisher.publishPaymentEvent("card.created", toPaymentDTO(payment));
        notifySuccess();
    }

    public void resolveFailed(Payment payment, String reason) {
        boolean applied = paymentPersistenceService.failIfStillLive(payment.getId(), reason);
        if (!applied) {
            logger.debug("Payment {} already resolved, ignoring failure resolution", payment.getId());
            return;
        }
        paymentPublisher.publishPaymentEvent("card.failed", toPaymentDTO(payment));
    }

    private PaymentDTO toPaymentDTO(Payment payment) {
        // CardPaymentDTO, not the bare PaymentDTO: @JsonTypeInfo's discriminator
        // shares the "type" property name with PaymentDTO's own `type` field, so
        // serializing the base class overwrites it with the class name instead of
        // the registered subtype id. This resolver only ever handles CARD anyway.
        PaymentDTO dto = new CardPaymentDTO();
        dto.setCartId(payment.getCartId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setType(payment.getPaymentType());
        return dto;
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
}
