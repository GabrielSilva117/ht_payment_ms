package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.DebitCardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DebitCartProcessor implements PaymentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DebitCartProcessor.class);

    @Override
    public void process(PaymentDTO payment) {
        DebitCardPaymentDTO debitPayment = (DebitCardPaymentDTO) payment;

    }

    private void validateDailyLimit(DebitCardPaymentDTO payment) {
        logger.debug("Checking credit limit for payment {}", payment.getCartId());
    }

    @Override
    public PaymentType getSupportedType() {
        return PaymentType.CARD_DEBIT;
    }
}
