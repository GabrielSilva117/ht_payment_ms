package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.dto.CardPaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;

public interface PaymentProcessor {
    void process (CardPaymentDTO payment);
    PaymentType getSupportedType();
}
