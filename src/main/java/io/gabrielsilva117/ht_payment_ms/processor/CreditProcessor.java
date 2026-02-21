package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.dto.CardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.CreditCardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.CardType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class CreditCardProcessor implements CardPaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CreditCardProcessor.class);


    @Override
    public void process(CardPaymentDTO payment) {
        CreditCardPaymentDTO creditPayment = (CreditCardPaymentDTO) payment;

        validateCreditLimit(creditPayment);
    }

    @Override
    public CardType getSupportedType() {
        return CardType.CREDIT;
    }

    private void validateCreditLimit(CreditCardPaymentDTO payment) {
        logger.debug("Checking credit limit for payment {}", payment.getPaymentId());
    }
}
