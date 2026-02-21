package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.dto.CardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.CreditCardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class CreditProcessor implements PaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CreditProcessor.class);


    @Override
    public void process(CardPaymentDTO payment) {
        CreditCardPaymentDTO creditPayment = (CreditCardPaymentDTO) payment;

        validateCreditLimit(creditPayment);
    }

    @Override
    public PaymentType getSupportedType() {
        return PaymentType.CARD_CREDIT;
    }

    private void validateCreditLimit(CreditCardPaymentDTO payment) {
        logger.debug("Checking credit limit for payment {}", payment.getCartId());
    }
}
