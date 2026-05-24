package io.gabrielsilva117.ht_payment_ms.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gabrielsilva117.ht_payment_ms.enums.PaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,              // Use a logical type name
        include = JsonTypeInfo.As.PROPERTY,      // Include as a JSON property
        property = "type"                 // Property name in JSON
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentDTO.class, name = "CARD"),
})
@Data
public class PaymentDTO {
    BigDecimal amount;
    String currency;
    PaymentType type;
    UUID cartId;
}