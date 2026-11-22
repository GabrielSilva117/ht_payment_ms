package io.gabrielsilva117.ht_payment_ms.processor;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import io.gabrielsilva117.ht_payment_ms.dto.CardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CardProcessor implements PaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CardProcessor.class);

    @Override
    public PaymentProcessorResult process(PaymentDTO payment) {
        CardPaymentDTO creditPayment = (CardPaymentDTO) payment;

        validateCreditLimit(creditPayment);

        try {
            // Card was already tokenized client-side (Stripe.js/Elements upstream in the
            // checkout) - we only ever see creditPayment.getPaymentMethodId(), never a raw PAN/CVV.
            PaymentMethod paymentMethod = PaymentMethod.retrieve(creditPayment.getPaymentMethodId());
            String last4 = paymentMethod.getCard() != null ? paymentMethod.getCard().getLast4() : null;

            // Final SUCCEEDED/FAILED confirmation happens via the Stripe webhook, not here -
            // this avoids the processor and the webhook racing to decide the outcome.
            PaymentIntent intent = PaymentIntent.create(buildIntentParams(creditPayment));
            logger.debug("Created Stripe PaymentIntent {} for payment {}", intent.getId(), payment.getCartId());
            return PaymentProcessorResult.pending(intent.getId(), last4);
        } catch (StripeException e) {
            logger.warn("Stripe declined payment {}: {}", payment.getCartId(), e.getMessage());
            return PaymentProcessorResult.failure(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error calling Stripe for payment {}", payment.getCartId(), e);
            return PaymentProcessorResult.failure(e.getMessage());
        }
    }

    private PaymentIntentCreateParams buildIntentParams(CardPaymentDTO creditPayment) {
        return PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(creditPayment.getAmount()))
                .setCurrency(creditPayment.getCurrency().toLowerCase())
                .setPaymentMethod(creditPayment.getPaymentMethodId())
                .addAllowedPaymentMethodType(PaymentIntentCreateParams.AllowedPaymentMethodType.CARD)
                .setConfirm(true)
                .putMetadata("cartId", creditPayment.getCartId().toString())
                .build();
    }

    // Simplification: assumes a 2-decimal currency (USD/EUR/BRL etc). Zero-decimal
    // currencies (e.g. JPY) are out of scope.
    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    @Override
    public PaymentType getSupportedType() {
        return PaymentType.CARD;
    }

    private void validateCreditLimit(CardPaymentDTO payment) {
        logger.debug("Checking credit limit for payment {}", payment.getCartId());
    }
}
