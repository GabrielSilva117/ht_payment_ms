package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CardPaymentDTO extends PaymentDTO {
    private String cardNumber;
    private String CVV;
    private String cardHolderName;
    private String expirationDate;

    public CardPaymentDTO() {
        this.setType(PaymentType.CARD);
    }
}
