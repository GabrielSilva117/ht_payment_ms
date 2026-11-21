package io.gabrielsilva117.ht_payment_ms.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import io.gabrielsilva117.ht_payment_ms.config.StripeProperties;
import io.gabrielsilva117.ht_payment_ms.service.PaymentOutcomeResolver;
import io.gabrielsilva117.ht_payment_ms.service.PaymentPersistenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment/webhook")
public class StripeWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final PaymentPersistenceService paymentPersistenceService;
    private final PaymentOutcomeResolver paymentOutcomeResolver;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            logger.warn("Invalid Stripe webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> handleSucceeded(event);
                case "payment_intent.payment_failed", "payment_intent.canceled" -> handleFailed(event);
                default -> logger.debug("Ignoring unhandled Stripe event type {}", event.getType());
            }
        } catch (Exception e) {
            // Logged and swallowed: a bug on our side must not make Stripe retry forever.
            logger.error("Error processing Stripe webhook event {}", event.getId(), e);
        }

        return ResponseEntity.ok("received");
    }

    private void handleSucceeded(Event event) {
        PaymentIntent intent = deserializeIntent(event);
        if (intent == null) {
            return;
        }

        paymentPersistenceService.findByGatewayReference(intent.getId()).ifPresentOrElse(
                payment -> paymentOutcomeResolver.resolveSucceeded(payment, intent.getId()),
                () -> logger.warn("No payment found for PaymentIntent {}", intent.getId()));
    }

    private void handleFailed(Event event) {
        PaymentIntent intent = deserializeIntent(event);
        if (intent == null) {
            return;
        }

        String reason = intent.getLastPaymentError() != null
                ? intent.getLastPaymentError().getMessage()
                : "Stripe event for PaymentIntent " + intent.getId();
        paymentPersistenceService.findByGatewayReference(intent.getId()).ifPresentOrElse(
                payment -> paymentOutcomeResolver.resolveFailed(payment, reason),
                () -> logger.warn("No payment found for PaymentIntent {}", intent.getId()));
    }

    private PaymentIntent deserializeIntent(Event event) {
        Object stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(stripeObject instanceof PaymentIntent intent)) {
            logger.warn("Could not deserialize PaymentIntent for event {}", event.getId());
            return null;
        }
        return intent;
    }
}
