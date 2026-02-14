package io.gabrielsilva117.ht_payment_ms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDTO {
    BigDecimal amount;
    String currency;
}
