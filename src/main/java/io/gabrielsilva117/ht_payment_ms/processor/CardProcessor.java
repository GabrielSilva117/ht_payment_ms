package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.dto.CardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class CardProcessor implements PaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CardProcessor.class);


    @Override
    public void process(PaymentDTO payment) {
        CardPaymentDTO creditPayment = (CardPaymentDTO) payment;

        validateCreditLimit(creditPayment);
    }

    @Override
    public PaymentType getSupportedType() {
        return PaymentType.CARD;
    }

    private void validateCreditLimit(CardPaymentDTO payment) {
        logger.debug("Checking credit limit for payment {}", payment.getCartId());
    }
}
