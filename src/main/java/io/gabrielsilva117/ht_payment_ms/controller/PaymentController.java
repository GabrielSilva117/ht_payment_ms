package io.gabrielsilva117.ht_payment_ms.controller;

import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentResponse;
import io.gabrielsilva117.ht_payment_ms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @PostMapping()
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentDTO body) {
        try {
            PaymentResponse res = paymentService.processPayment(body);
            log.info("Processed payment: {}", res);
            return ResponseEntity.ok().body(res);
        } catch (Exception e) {
            log.error("Failed to process payment for cart {}", body.getCartId(), e);
            return ResponseEntity.status(500).build();
        }

    }
}
