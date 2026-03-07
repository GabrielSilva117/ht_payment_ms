package io.gabrielsilva117.ht_payment_ms.processor;

import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;

public interface PaymentProcessor {
    void process (PaymentDTO payment);
    PaymentType getSupportedType();
}
