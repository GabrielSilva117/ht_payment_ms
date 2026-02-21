package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.CardType;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardPaymentDTO {
    private PaymentType type;
    private UUID cartId;
}
