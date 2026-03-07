package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CreditCardPaymentDTO extends PaymentDTO {
    private String creditLimit;

    public CreditCardPaymentDTO () {
        this.setType(PaymentType.CARD_CREDIT);
    }
}
