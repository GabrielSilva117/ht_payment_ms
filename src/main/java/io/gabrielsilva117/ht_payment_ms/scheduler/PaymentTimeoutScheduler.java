package io.gabrielsilva117.ht_payment_ms.scheduler;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.gabrielsilva117.ht_payment_ms.entity.Payment;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentEntityStatus;
import io.gabrielsilva117.ht_payment_ms.service.PaymentOutcomeResolver;
import io.gabrielsilva117.ht_payment_ms.service.PaymentPersistenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Cards without a terminal Stripe webhook can otherwise stay PENDING/PROCESSING forever
 * (lost webhook, Stripe-side incident, etc). This sweep resolves any such row past the
 * timeout so the cart isn't stuck - a resolved row no longer blocks a retry, since
 * uk_payments_cart_active excludes FAILED rows.
 *
 * A row with no gatewayReference never completed the initial Stripe call (e.g. we crashed
 * mid-request) - there is nothing to check, it's just failed. A row with a gatewayReference
 * has a real PaymentIntent at Stripe: silence on our end doesn't mean Stripe failed it too -
 * the webhook relay (the local stripe-cli tunnel, in dev) can simply drop the event while
 * Stripe already captured the charge. So before giving up on that row, ask Stripe directly
 * what actually happened, and resolve to whatever Stripe says instead of assuming failure.
 */
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PaymentTimeoutScheduler.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private static final List<PaymentEntityStatus> LIVE_STATUSES =
            List.of(PaymentEntityStatus.PENDING, PaymentEntityStatus.PROCESSING);
    private static final Set<String> STRIPE_FAILED_STATUSES = Set.of("canceled", "requires_payment_method");

    private final PaymentPersistenceService paymentPersistenceService;
    private final PaymentOutcomeResolver paymentOutcomeResolver;

    @Scheduled(fixedRate = 15_000)
    public void resolveStalePayments() {
        Instant cutoff = Instant.now().minus(TIMEOUT);
        List<Payment> stale = paymentPersistenceService.findStale(LIVE_STATUSES, cutoff);

        for (Payment payment : stale) {
            if (payment.getGatewayReference() == null) {
                // Never reached Stripe - nothing to reconcile against.
                boolean failed = paymentPersistenceService.timeoutFail(payment.getId(), timeoutReason());
                if (failed) {
                    logger.warn("Payment {} (cart {}) timed out before a gateway call completed", payment.getId(), payment.getCartId());
                }
                continue;
            }

            reconcileWithStripe(payment);
        }
    }

    private void reconcileWithStripe(Payment payment) {
        PaymentIntent intent;
        try {
            intent = PaymentIntent.retrieve(payment.getGatewayReference());
        } catch (StripeException e) {
            logger.error("Failed to reconcile payment {} (cart {}) against Stripe, will retry next sweep",
                    payment.getId(), payment.getCartId(), e);
            return;
        }

        String status = intent.getStatus();
        if ("succeeded".equals(status)) {
            logger.warn("Payment {} (cart {}) had no webhook but Stripe already succeeded it - resolving from reconciliation",
                    payment.getId(), payment.getCartId());
            paymentOutcomeResolver.resolveSucceeded(payment, intent.getId());
        } else if (STRIPE_FAILED_STATUSES.contains(status)) {
            String reason = intent.getLastPaymentError() != null
                    ? intent.getLastPaymentError().getMessage()
                    : "Stripe PaymentIntent status: " + status;
            paymentOutcomeResolver.resolveFailed(payment, reason);
        } else {
            logger.debug("Payment {} (cart {}) still non-terminal at Stripe ({}), leaving for next sweep",
                    payment.getId(), payment.getCartId(), status);
        }
    }

    private String timeoutReason() {
        return "Payment timed out after " + TIMEOUT.toSeconds() + "s without gateway confirmation";
    }
}
