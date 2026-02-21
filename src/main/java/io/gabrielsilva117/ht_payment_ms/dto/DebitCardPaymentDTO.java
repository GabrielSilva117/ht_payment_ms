package io.gabrielsilva117.ht_payment_ms.dto;

import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;

import java.math.BigDecimal;

public class DebitCardPaymentDTO extends CardPaymentDTO {
    private BigDecimal dailyLimit;


    public DebitCardPaymentDTO () {
        this.setType(PaymentType.CARD_DEBIT);
    }
}
