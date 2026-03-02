package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentDTO {
    BigDecimal amount;
    String currency;
    PaymentType type;
    UUID cartId;
}
