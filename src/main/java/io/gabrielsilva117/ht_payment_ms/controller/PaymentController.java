package io.gabrielsilva117.ht_payment_ms.controller;

import io.gabrielsilva117.ht_payment_ms.dto.PaymentDTO;
import io.gabrielsilva117.ht_payment_ms.dto.PaymentResponse;
import io.gabrielsilva117.ht_payment_ms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping()
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentDTO body) {
        try {
            PaymentResponse res = paymentService.processPayment(body);
            System.out.println(res);
            return ResponseEntity.ok().body(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }

    }
}
