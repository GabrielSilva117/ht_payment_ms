package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {
    PaymentStatus status;
}
