package io.gabrielsilva117.ht_payment_ms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDTO {
    private String subject;
    private String body;
    private String to;
}
