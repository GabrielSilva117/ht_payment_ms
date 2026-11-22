package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardPaymentDTO extends PaymentDTO {
    private String paymentMethodId;

    public CardPaymentDTO() {
        this.setType(PaymentType.CARD);
    }
}
